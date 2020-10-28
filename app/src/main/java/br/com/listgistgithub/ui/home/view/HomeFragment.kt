package br.com.listgistgithub.ui.home.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.listgistgithub.R
import br.com.listgistgithub.data.api.ApiHelper
import br.com.listgistgithub.data.api.RetrofitBuilder
import br.com.listgistgithub.model.Gist
import br.com.listgistgithub.ui.base.ViewModelFactory
import br.com.listgistgithub.ui.home.adapter.HomeAdapter
import br.com.listgistgithub.ui.home.viewmodel.HomeViewModel
import br.com.listgistgithub.utils.Status
import kotlinx.android.synthetic.main.fragment_home.*

class HomeFragment : Fragment() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: HomeAdapter
    private var pastVisiblesItems = 0
    private var totalItemCount = 0
    private var loading = false
    private var pageLoad = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        setupViewModel()

        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerView()
        setupRecyclerViewAdapter()
        viewModel.getFavorites(requireActivity())
        setupObservers()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelFactory(ApiHelper(RetrofitBuilder.apiService))
        ).get(HomeViewModel::class.java)
    }

    private fun setupRecyclerView() {
        recyclerViewGists.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewGists.setHasFixedSize(true)

        recyclerViewGists.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(
                recyclerView: RecyclerView, dx: Int, dy: Int
            ) {
                if (dy > 0) {
                    totalItemCount = recyclerViewGists.layoutManager!!.itemCount
                    pastVisiblesItems =
                        (recyclerViewGists.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()

                    if (!loading) {
                        if (pastVisiblesItems >= totalItemCount - 1) {
                            loading = true
                            pageLoad += 1
                            viewModel.getFavorites(requireActivity())
                            setupObservers()
                        }
                    }
                }
            }
        })
    }

    private fun setupRecyclerViewAdapter() {
        adapter = HomeAdapter(requireContext(), arrayListOf()) {
            val action = HomeFragmentDirections.actionHomeFragmentToDetailsFragment(
                it.owner!!.login!!, it.owner!!.avatarUrl!!, it.description!!
            )
            requireView().findNavController().navigate(action)
        }
        adapter.setOnFavoriteClickListener {
            viewModel.insertFavorite(requireContext(), it)
        }
        adapter.setOnUnFavoriteClickListener {
            viewModel.deleteFavorite(requireContext(), it.id!!)
        }
        recyclerViewGists.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.getGirts(pageLoad, 30).observe(requireActivity(), Observer {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        progressBar.visibility = View.GONE
                        resource.data?.let { gists -> retrieveList(gists) }
                        loading = false
                    }
                    Status.ERROR -> {
                        progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), it.message, Toast.LENGTH_LONG).show()
                        loading = false
                    }
                    Status.LOADING -> {
                        progressBar.visibility = View.VISIBLE
                    }
                }
            }
        })
    }

    private fun retrieveList(gists: List<Gist>) {
        viewModel.setGistsFavorites(gists)
        adapter.apply {
            addGist(gists)
            notifyDataSetChanged()
        }
    }
}