package com.bromne.async

class ParallelAsyncLoader<K, T> {
    internal val tasks: MutableMap<K, RegularAsyncTask<T>> = mutableMapOf()
    internal val listenersDictionary: MutableMap<K, MutableList<RegularAsyncTask.Callbacks<T>>> = mutableMapOf()

    fun loadOrRegister(id: K, callbacks: RegularAsyncTask.Callbacks<T>) {
        if (!this.listenersDictionary.containsKey(id))
            this.listenersDictionary.put(id, mutableListOf())

        val listeners = this.listenersDictionary[id]!!
        listeners.add(callbacks)

        callbacks.onPreLoad()

        if (!tasks.containsKey(id)) {
            val emitter = object : RegularAsyncTask.Callbacks<T> {
                override fun loadInBackground(publishProgress: (Int) -> Unit): T = callbacks.loadInBackground(publishProgress)

                override fun onLoadFinished(result: T) {
                    listeners.forEach { it.onLoadFinished(result) }
                    tasks.remove(id)
                    listeners.clear()
                }

                override fun onException(e: Exception) {
                    listeners.forEach { it.onException(e) }
                    tasks.remove(id)
                    listeners.clear()
                }

                override fun onCancelled() {
                    listeners.forEach { it.onCancelled() }
                    tasks.remove(id)
                    listeners.clear()
                }

                override fun onProgressUpdate(progress: Int) = listeners.forEach { it.onProgressUpdate(progress) }
            }

            val task = RegularAsyncTask(emitter)
            tasks[id] = task
            task.execute()
        }
    }
}
