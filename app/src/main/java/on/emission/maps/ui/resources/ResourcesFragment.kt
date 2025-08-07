package on.emission.maps.ui.resources

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import on.emission.maps.R
import on.emission.maps.data.MainViewModel

class ResourcesFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ResourcesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_resources, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.resources_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        viewModel.parsedUrls.observe(viewLifecycleOwner) { urls ->
            adapter = ResourcesAdapter(urls)
            recyclerView.adapter = adapter
        }

        viewModel.fetchParsedUrls()
    }
}
