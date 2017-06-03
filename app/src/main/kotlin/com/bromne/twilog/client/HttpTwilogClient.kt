package com.bromne.twilog.client

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.bromne.stereotypes.data.excludeNullable
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.net.URL
import java.util.ArrayDeque
import java.util.regex.Pattern
import com.bromne.twilog.client.TwilogClient.Joint
import com.bromne.twilog.client.TwilogClient.Order

class HttpTwilogClient : TwilogClient {

    internal val iconCache: MutableMap<String, Bitmap> = mutableMapOf()

    override fun find(query: TwilogClient.Query): Result {
        val base = "http://twilog.org/${query.userName}"
        val request = query.body.map({
            val date = it?.let { "/date-" + it.toString("yy-MM-dd") }
            val order = if (query.order == Order.ASC) "/allasc" else ""
            (date ?: "") + order
        }, {
            val order = if (query.order == Order.ASC) "order=allasc" else null

            // TODO: 検索文字列のサニタイズ
            val queryString = listOf("word=" + it.keyword, "ao=" + it.joint, order)
                    .excludeNullable()
                    .joinToString("&")

            "/search?" + queryString
        })

        val url = base + request
        return extractResultByUrl(url)
    }

    override fun findRecent(userName: String): Result {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun findByDate(userName: String): Result {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun search(userName: String, query: String, joint: Joint): Result {
        val url = "http://twilog.org/$userName/search?word=$query&ao=$joint"
        return extractResultByUrl(url)
    }

    override fun loadUserIcon(user: User): Bitmap {
        if (this.iconCache.contains(user.image.bigger)) {
            return iconCache[user.image.bigger]!!
        } else {
            val url = URL(user.image.bigger)
            val bitmap = BitmapFactory.decodeStream(url.openStream())
            iconCache[user.image.bigger]
            return bitmap
        }
    }

    fun extractResultByUrl(url: String): Result {
        val timeFormat = Pattern.compile("(\\d{2}:\\d{2}:\\d{2})")
        val nameFormat = Pattern.compile("@(.+)")
        val dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")

        fun tweeterFromElement(root: Element): User {
            val name = root.select("#user-info-content h1 span").text()
                    .extractWithPattern(nameFormat)
            val displayName = root.select("#user-info-content h1 strong").text()
            val image = root.select("#user-info-icon img")
                    .attr("src")
                    .let { UserImage.fromBigger(it) }
            return User(name ?: "", displayName, image)
        }

        fun extractGroup(elements: Elements): List<Pair<LocalDate, List<Element>>> {
            val datePattern = Pattern.compile("(\\d{4}年\\d{2}月\\d{2}日)")
            val df = DateTimeFormat.forPattern("yyyy年MM月dd日")

            val queue = ArrayDeque(elements)
            val groupedByDate: MutableList<Pair<LocalDate, List<Element>>> = mutableListOf()

            var date: Element? = null
            var tweets: MutableList<Element> = mutableListOf()

            while (true) {
                val element = queue.poll()
                if (element == null || element.tagName() == "h3") {
                    if (date != null) {
                        groupedByDate.add(Pair(df.parseLocalDate(date.text().extractWithPattern(datePattern)), tweets.toList()))
                    }
                    date = element
                    tweets = mutableListOf()
                } else {
                    tweets.add(element)
                }

                if (element == null)
                    break
            }

            return groupedByDate.toList()
        }

        fun tweetFromElement(date: LocalDate, element: Element, users: Map<String, User>): Tweet? {
            val time = element.select(".tl-posted")
                    .text()
                    .extractWithPattern(timeFormat)

            if (time == null)
                return null

            val status = element.select(".tl-time a")
                    .attr("href")
            val created = dateTimeFormat.parseLocalDateTime("${date.toString("yyyy-MM-dd")} $time")
            val name = element.select(".tl-name span").text().extractWithPattern(nameFormat)
            val message = element.select(".tl-text").text()
            return Tweet(status, users[name]!!, created, message)
        }

        val ua = System.getProperty("http.agent")
        val document = Jsoup.connect(url).header("User-Agent", ua).get()

        val tweeter = tweeterFromElement(document)

        val tweetElements = document.select("#content")
                .select("h3.title01,section.tl-tweets article.tl-tweet")

        val groups = extractGroup(tweetElements)

        val userDictionary: Map<String, User> = groups.flatMap { it.second }
                .distinctBy { it.select(".tl-name span").text() }
                .map {
                    val userName = it.select(".tl-name span").text().extractWithPattern(nameFormat)
                    val displayName = it.select(".tl-name strong").text()
                    val image = it.select(".tl-icon a img").attr("src")
                    if (userName != null) User(userName, displayName, UserImage.fromNormal(image)) else null
                }.excludeNullable()
                .associateBy( User::name )

        val tweets = groups
                .map { group ->
                    val date = group.first
                    group.second.map { element -> tweetFromElement(date, element, userDictionary) }
                }
                .flatMap { it.toList() }
                .excludeNullable()
                .toList()

        return Result(tweeter, tweets)
    }
}
