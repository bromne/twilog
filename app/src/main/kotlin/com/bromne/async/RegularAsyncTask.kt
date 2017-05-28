package com.bromne.async

import android.os.AsyncTask
import com.bromne.data.Either

class RegularAsyncTask<T>(val callbacks: Callbacks<T>) : AsyncTask<Void, Int, Either<Exception, T>>() {
    override fun doInBackground(vararg params: Void?): Either<Exception, T> {
        try {
            val data = this.callbacks.loadInBackground({ publishProgress(it) })
            return Either.right(data)
        } catch (e: Exception) {
            return Either.left(e)
        }
    }

    override fun onPreExecute(): Unit {
        this.callbacks.onPreLoad()
    }

    override fun onCancelled(): Unit {
        this.callbacks.onCancelled()
    }

    override fun onProgressUpdate(vararg values: Int?): Unit  {
        this.callbacks.onProgressUpdate(values[0]!!)
    }

    override fun onPostExecute(result: Either<Exception, T>): Unit {
        when (result) {
            is Either.Right -> this.callbacks.onLoadFinished(result.value)
            is Either.Left -> this.callbacks.onException(result.value)
        }

        result.map({ this.callbacks.onException(it) }, { this.callbacks.onLoadFinished(it) })

//        if (result.isRight) {
//            this.callbacks.onLoadFinished(result.right().value())
//        } else {
//            this.callbacks.onException(result.left().value())
//        }
    }

    companion object {
        fun <T> execute(callbacks: Callbacks<T>): Unit {
            RegularAsyncTask(callbacks).execute()
        }
    }

    interface Callbacks<T> {
        fun onPreLoad(): Unit {
            // do nothing by default.
        }

        fun loadInBackground(publishProgress: (Int) -> Unit): T

        fun onLoadFinished(result: T): Unit

        fun onException(e: Exception): Unit

        fun onCancelled(): Unit {
            // do nothing by default.
        }

        fun onProgressUpdate(progress: Int): Unit {
            // do nothing by default.
        }
    }
}

