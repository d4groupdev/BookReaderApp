package com.devforfun.example.ui.home.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.devforfun.example.Prefs
import com.devforfun.example.R
import com.devforfun.example.model.recommendedCategories.RecommendedCategories
import com.devforfun.example.model.recommendedCategories.RecommendedCategoryBooks
import com.devforfun.example.model.search.Data
import com.devforfun.example.model.search.Search
import com.devforfun.example.network.NetworkManager
import com.devforfun.example.network.NetworkState
import com.devforfun.example.ui.home.adapter.RecommendedCategoriesMoreAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.fragment_recommended_categories.*
import kotlinx.android.synthetic.main.fragment_recommended_categories.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RecommendedCategoriesFragment : Fragment(), RecommendedCategoriesMoreAdapter.Callback {

    private var categoriesList: ArrayList<Data> =
        ArrayList()
    private var booksList: ArrayList<Data> = ArrayList()
    private var categoryWithBookList: ArrayList<RecommendedCategoryBooks> = ArrayList()

    private lateinit var prefs: Prefs

    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewAdapter: RecommendedCategoriesMoreAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View =
            inflater.inflate(R.layout.fragment_recommended_categories, container, false)
        prefs = Prefs(requireContext())

        recyclerViewAdapter =
            RecommendedCategoriesMoreAdapter(categoryWithBookList, requireContext(), this)
        recyclerView = view.rvRecommendedCategoriesList.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recyclerViewAdapter
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbarRecommendedCategories.setupWithNavController(
            findNavController(),
            AppBarConfiguration(findNavController().graph)
        )

        toolbarRecommendedCategories.title = resources.getText(R.string.recommended_categories)
        val navView: BottomNavigationView =
            requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        navView.visibility = View.VISIBLE

        NetworkState.getInstance().runWhenNetworkAvailable {
            pbRecommendedCategories?.visibility = View.VISIBLE
            updateBooks(0)
            updateCategories()
        }.showNetworkNotAvailableMessage(getString(R.string.internet_not_available))

    }


    private fun updateCategories() {
        val categoriesRetrofit =
            NetworkManager.apiService.recommendedCategories("Bearer ${prefs.getString("token")}")!!
        categoriesRetrofit.enqueue(object : Callback<RecommendedCategories> {
            override fun onResponse(
                call: Call<RecommendedCategories>,
                response: Response<RecommendedCategories>
            ) {
                if (response.body() != null && response.body()!!.success) {
                    categoriesList.clear()
                    categoriesList.addAll(response.body()!!.data)
                    if (booksList.isNotEmpty() && categoriesList.isNotEmpty()) {
                        prepareCategoryWithBookList()
                    }
                } else if (response.body() != null && context != null) {
                    Toast.makeText(context, response.body()!!.message, Toast.LENGTH_LONG).show()
                    pbRecommendedCategories?.visibility = View.GONE
                }
            }

            override fun onFailure(call: Call<RecommendedCategories>, t: Throwable) {
                Toast.makeText(requireContext(), t.message, Toast.LENGTH_SHORT).show()
                pbRecommendedCategories.visibility = View.GONE
            }
        })

    }

    private fun updateBooks(page: Int) {
        val booksRetrofit =
            NetworkManager.apiService.getBookList("Bearer ${prefs.getString("token")}", page)!!
        booksRetrofit.enqueue(object : Callback<Search> {
            override fun onResponse(call: Call<Search>, response: Response<Search>) {
                if (response.body() != null && response.body()!!.success) {

                    booksList.addAll(response.body()!!.data)

                    if (response.body()!!.meta.current_page < response.body()!!.meta.last_page) {
                        updateBooks(response.body()!!.meta.current_page + 1)
                    } else {
                        if (booksList.isNotEmpty() && categoriesList.isNotEmpty()) {
                            prepareCategoryWithBookList()
                        }
                    }

                } else if (response.body() != null && context != null) {
                    Toast.makeText(context, response.body()!!.message, Toast.LENGTH_LONG).show()
                    pbRecommendedCategories.visibility = View.GONE
                }

            }

            override fun onFailure(call: Call<Search>, t: Throwable) {
                Toast.makeText(requireContext(), t.message, Toast.LENGTH_SHORT).show()
                pbRecommendedCategories.visibility = View.GONE
            }

        })
    }

    private fun prepareCategoryWithBookList() {
        categoryWithBookList.clear()
        GlobalScope.launch {
            for (category in categoriesList) {
                categoryWithBookList.add(RecommendedCategoryBooks(category, ArrayList<Data>()))
            }

            for (book in booksList) {
                for (bookCategory in book.category) {
                    for (category in categoryWithBookList) {
                        if (bookCategory.Id == category.category.id) {
                            category.books.add(book)
                            break
                        }
                    }
                }
            }

            var categoryWithBooks: ArrayList<RecommendedCategoryBooks> = ArrayList()

            for (categ in categoryWithBookList) {
                if (categ.books.isNotEmpty()) {
                    categoryWithBooks.add(categ)
                }
            }

            categoryWithBookList.clear()
            categoryWithBookList.addAll(categoryWithBooks)

            activity?.runOnUiThread {
                recyclerViewAdapter.notifyDataSetChanged()
                pbRecommendedCategories.visibility = View.GONE
            }

        }
    }

    override fun onItemClickedRecomendedCategories(data: Data) {
        prefs.putString("idBook", data.id.toString())
        view?.findNavController()?.navigate(R.id.bookFragment)
    }
}