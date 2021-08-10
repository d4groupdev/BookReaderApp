package com.devforfun.example.ui.home.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.devforfun.example.Prefs
import com.devforfun.example.R
import com.devforfun.example.model.search.Data
import com.devforfun.example.model.search.Search
import com.devforfun.example.model.search.favorites.SearchFavorites
import com.devforfun.example.network.NetworkManager
import com.devforfun.example.network.NetworkState
import com.devforfun.example.ui.home.adapter.FreeContentAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.fragment_free_content.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FreeContentFragment : Fragment(), FreeContentAdapter.Callback {
    private val modelFree: ArrayList<Data> = ArrayList()
    lateinit var recyclerAdapter: FreeContentAdapter
    private val api = NetworkManager.apiService
    private lateinit var prefs: Prefs
    private var currentPage = 0
    private var maxPages = 0
    private var isGettingInfo = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_free_content, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = Prefs(requireContext())

        toolbarFreeContent.setupWithNavController(
            findNavController(),
            AppBarConfiguration(findNavController().graph)
        )

        toolbarFreeContent.title = resources.getText(R.string.free_content)

        val navView: BottomNavigationView =
            requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        navView.visibility = View.VISIBLE

        recyclerAdapter = FreeContentAdapter(modelFree, requireContext(), this)

        rwFreeContent.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recyclerAdapter
        }

        NetworkState.getInstance().runWhenNetworkAvailable {
            pbFreeContent?.visibility = View.VISIBLE
            getContent()
        }.showNetworkNotAvailableMessage(getString(R.string.internet_not_available))

    }

    private fun getContent() {
        isGettingInfo = true
        val freeContent = NetworkManager.apiService.getListBookByType(
            "Bearer ${prefs.getString("token")}",
            "free",
            currentPage + 1
        )
        freeContent?.enqueue(object : Callback<Search> {
            override fun onResponse(call: Call<Search>, response: Response<Search>) {
                if (response.body() != null && response.body()!!.success) {
                    recyclerAdapter.setMovieListItems(response.body()?.data!!)
                    currentPage = response.body()!!.meta.current_page
                    maxPages = response.body()!!.meta.last_page
                } else if (response.body() != null && context != null) {
                    Toast.makeText(context, response.body()!!.message, Toast.LENGTH_LONG).show()
                }
                pbFreeContent?.visibility = View.GONE
                isGettingInfo = false
            }

            override fun onFailure(call: Call<Search>, t: Throwable) {
                pbFreeContent?.visibility = View.GONE
                isGettingInfo = false
            }
        })
    }

    override fun onItemClicked(data: Data) {
        if (!NetworkState.getInstance().isInternetAvailable()) {
            NetworkState.getInstance()
                .showNetworkNotAvailableMessage(getString(R.string.internet_not_available))
            return
        }
        prefs.putString("idBook", data.id.toString())
        val action = FreeContentFragmentDirections.actionGlobalBookFragment()
        findNavController().navigate(action)
    }

    override fun onFavorites(data: Data) {
        if (!NetworkState.getInstance().isInternetAvailable()) {
            NetworkState.getInstance()
                .showNetworkNotAvailableMessage(getString(R.string.internet_not_available))
            return
        }
        pbFreeContent?.visibility = View.VISIBLE
        data.is_favorites = data.is_favorites.not()
        val isFavor = if (data.is_favorites) 1 else 0
        val favorite = api.searchFavorites(
            "Bearer ${prefs.getString("token")}",
            data.id.toString(),
            isFavor
        )

        favorite?.enqueue(object : Callback<SearchFavorites> {
            override fun onResponse(
                call: Call<SearchFavorites>,
                response: Response<SearchFavorites>
            ) {
                if (response.body() != null && context != null && !response.body()!!.success) {
                    Toast.makeText(context, response.body()!!.message, Toast.LENGTH_LONG).show()
                }
                pbFreeContent?.visibility = View.GONE
            }

            override fun onFailure(call: Call<SearchFavorites>, t: Throwable) {
                pbFreeContent?.visibility = View.GONE
            }
        })
    }

    override fun onMaxPosition() {
        NetworkState.getInstance().runWhenNetworkAvailable {
            getBookList()
        }.showNetworkNotAvailableMessage(getString(R.string.internet_not_available))
    }

    private fun getBookList() {
        if (isGettingInfo || maxPages == currentPage || maxPages == 0 || currentPage == 0) return
        isGettingInfo = true
        pbFreeContent?.visibility = View.VISIBLE
        val retrofitGetBookOnPage = NetworkManager.apiService.getListBookByType(
            "Bearer ${prefs.getString("token")}",
            "free",
            currentPage + 1
        )
        retrofitGetBookOnPage!!.enqueue(object : Callback<Search> {
            override fun onResponse(call: Call<Search>, response: Response<Search>) {
                if (response.body() != null && response.body()!!.success) {
                    recyclerAdapter.addToList(response.body()?.data!!)
                    currentPage = response.body()!!.meta.current_page
                    activity?.runOnUiThread {
                        recyclerAdapter.notifyDataSetChanged()
                    }
                } else if (response.body() != null && context != null) {
                    Toast.makeText(context, response.body()!!.message, Toast.LENGTH_LONG).show()
                }
                pbFreeContent?.visibility = View.GONE
                isGettingInfo = false
            }

            override fun onFailure(call: Call<Search>, t: Throwable) {
                pbFreeContent?.visibility = View.GONE
                isGettingInfo = false
            }

        })
    }
}