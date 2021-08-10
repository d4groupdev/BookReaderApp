package com.devforfun.example.ui.library.fragment

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.devforfun.example.Prefs
import com.devforfun.example.R
import com.devforfun.example.download.model.DownloadResult
import com.devforfun.example.download.utils.Extensions
import com.devforfun.example.download.utils.globalContext
import com.devforfun.example.model.download.Download
import com.devforfun.example.ui.library.adapter.DownloadAdapter
import kotlinx.android.synthetic.main.fragment_download.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class DownloadFragment : Fragment() {
    private val modelDownload: ArrayList<Download> = ArrayList()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_download, container, false)
    }

    private lateinit var prefs: Prefs


    private val extension: Extensions = Extensions()

    private lateinit var myAdapter: DownloadAdapter
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = Prefs(requireContext())
        val listDownload = prefs.getDownloadList("Download")

        modelDownload.addAll(listDownload)
        if (modelDownload.isNullOrEmpty()) {
            download_placeholder.visibility = View.VISIBLE
        } else {
            download_placeholder.visibility = View.GONE
        }


        with(rwDownload) {
            layoutManager = LinearLayoutManager(requireContext())
            DividerItemDecoration(
                requireContext(),
                (layoutManager as LinearLayoutManager).orientation
            ).apply {
                addItemDecoration(this)
            }

            myAdapter = DownloadAdapter(
                listDownload.toList(),
                requireContext(),
                object : DownloadAdapter.DownloadAdapterListener {
                    override fun onItemClick(downloadItem: Download) {
                        Log.i("clickManager", "clickdownload")
                        prefs.putString("idBook", downloadItem.id)
                        val action = LibraryFragmentDirections.actionGlobalBookFragment()
                        findNavController().navigate(action)
                    }

                    override fun onItemDelete(downloadItem: Download) {
                        val path =
                            globalContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath
                        Log.d("Files", "Path: $path")
                        val directory = File(path)
                        val files = directory.listFiles()
                        Log.d("Files", "Size: " + files.size)
                        for (i in files.indices) {
                            Log.d("Files", "FileName:" + files[i].name)
                        }

                        Log.d("Download", "Url item: ${downloadItem.url}")
                    }
                })

            adapter = myAdapter
        }

        if (listDownload.size > 0 && !File(
                globalContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                listDownload.last().nameBook
            ).exists()
        )
            try {
                Log.i("clickManager", "download")
                downloadWithFlow(listDownload.last())
            } catch (e: Exception) {
                Log.i("clickManager", e.toString())
            }
    }

    private fun downloadWithFlow(dummy: Download) {
        CoroutineScope(Dispatchers.IO).launch {
            extension.downloadFile(
                File(
                    globalContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    dummy.nameBook
                ), dummy.url
            ).collect {
                withContext(Dispatchers.Main) {
                    when (it) {
                        is DownloadResult.Success -> {
                            myAdapter.setDownloading(dummy, true)
                        }
                        is DownloadResult.Error -> {
                            myAdapter.setDownloading(dummy, false)
                            Toast.makeText(
                                requireContext(),
                                "${resources.getText(R.string.error_while_downloading)} ${dummy.nameBook}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        is DownloadResult.Progress -> {
                            myAdapter.setProgress(dummy, it.progress)
                        }
                    }
                }
            }
        }
    }
}