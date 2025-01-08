package on.emission.maps.ui.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import on.emission.maps.databinding.FragmentMoreBinding
import on.emission.maps.ui.main.MainActivity

class MoreFragment : Fragment() {
  
  private lateinit var binding: FragmentMoreBinding
  private lateinit var myActivity:MainActivity
  
  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    myActivity = activity as MainActivity
    binding = FragmentMoreBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

  }
}