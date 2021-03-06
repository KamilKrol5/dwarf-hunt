package com.example.krasnalhunt


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.krasnalhunt.model.DwarfItem
import com.example.krasnalhunt.model.DwarfViewModel
import com.example.krasnalhunt.model.MainViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.search_dialog_view.view.*

class MainFragment : Fragment(), OnMapReadyCallback {

    private var listener: OnFragmentInteractionListener? = null
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    private val mainViewModel: MainViewModel by activityViewModels()
    private val dwarfViewModel: DwarfViewModel by activityViewModels()

    private var currentCircle: Circle? = null
    private val currentBehaviorState = MutableLiveData<Int>().apply { value = BottomSheetBehavior.STATE_HIDDEN }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mainViewModel.map = googleMap
        mainViewModel.filteredItems.observe(this, Observer { dwarfs ->
            Log.d("TAG", dwarfs.toString())
            mainViewModel.map.clear()
            currentCircle = null
            for (dwarf in dwarfs) {
                mainViewModel.map.addMarker(MarkerOptions().position(dwarf.coordinates).title(dwarf.name)).apply {
                    tag = dwarf
                }
            }
        })

        mainViewModel.map.setOnMarkerClickListener {
            currentCircle?.remove()
            currentCircle = mainViewModel.map.addCircle(
                CircleOptions()
                    .center(it.position)
                    .radius(30.0)
                    .strokeWidth(3.0f)
                    .strokeColor(Color.GREEN)
                    .fillColor(Color.argb(128, 255, 0, 0))
                    .clickable(false)
            )
            it.showInfoWindow()
            true
        }

        mainViewModel.map.setOnMapClickListener {
            currentCircle?.remove()
            currentCircle = null
        }

        mainViewModel.map.setOnInfoWindowClickListener {
            val item = it.tag as DwarfItem
            dwarfViewModel.dwarfItem = item

            val result = floatArrayOf(0f)
            Location.distanceBetween(
                mainViewModel.location.value!!.latitude, mainViewModel.location.value!!.longitude,
                item.coordinates.latitude, item.coordinates.longitude,
                result
            )
            dwarfViewModel.distance = result[0].toInt()
            val dwarfViewFragment = DwarfViewFragment()
            requireActivity().supportFragmentManager.commit {
                addToBackStack(null)
                replace(R.id.content, dwarfViewFragment, "dwarfView")
            }
        }

        mainViewModel.locationPermissionGranted.observe(this, Observer {
            mainViewModel.map.run {
                isMyLocationEnabled = it
                uiSettings.isMyLocationButtonEnabled = it
            }
        })

        mainViewModel.dwarfsWithDistance.observe(this, Observer {
            if (it.isEmpty()) {
                mainViewModel.searchString.value = ""
                showNotFoundDialog()
            }
        })
    }

    private fun showNotFoundDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage(R.string.nothing_found_message)
        builder.setPositiveButton(R.string.positive_button_text) { dialog, _ ->
            dialog.cancel()
        }.show().apply {
            val positiveButton = this.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setTextColor(resources.getColor(R.color.justBlack, null))
            positiveButton.background =
                resources.getDrawable(R.color.yellowBackground, null)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(BOTTOM_SHEET_BEHAVIOR_STATE, currentBehaviorState.value ?: BottomSheetBehavior.STATE_HIDDEN)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    private fun initViews() {
        childFragmentManager.commit {
            childFragmentManager.findFragmentByTag("map")?.let { mapFragment ->
                (mapFragment as SupportMapFragment).getMapAsync(this@MainFragment)
            } ?: run {
                val mapFragment = SupportMapFragment.newInstance(
                    GoogleMapOptions()
                        .camera(CameraPosition.fromLatLngZoom(LatLng(51.109286, 17.032307), 16.0f))
                        .maxZoomPreference(19.0f)
                )

                replace(R.id.content_map, mapFragment, "map")
                mapFragment.getMapAsync(this@MainFragment)
            }

            replace(R.id.content_list, DwarfItemListFragment.newInstance(), "list")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()

        val lp = content_list.layoutParams
        if (lp is CoordinatorLayout.LayoutParams) {
            val behavior = lp.behavior
            if (behavior is BottomSheetBehavior<*>) {
                bottomSheetBehavior = behavior
            }
        }

        currentBehaviorState.observe(this, Observer {
            when (it) {
                BottomSheetBehavior.STATE_HIDDEN ->
                    fab.setImageResource(R.drawable.ic_view_list_black_24dp)
                else ->
                    fab.setImageResource(R.drawable.ic_map_black_24dp)
            }
        })

        mainViewModel.searchString.observe(this, Observer {
            if (it == null || it == "") {
                searchButton.setImageResource(R.drawable.ic_search_black_24dp)
            } else {
                searchButton.setImageResource(R.drawable.ic_search_crossed_black_24px)
            }
        })

        bottomSheetBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(p0: View, p1: Float) = Unit

            override fun onStateChanged(p0: View, p1: Int) {
                currentBehaviorState.value = p1
            }
        })

        if (savedInstanceState?.containsKey(BOTTOM_SHEET_BEHAVIOR_STATE) == true) {
            bottomSheetBehavior.state = savedInstanceState.getInt(BOTTOM_SHEET_BEHAVIOR_STATE)
        } else {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        currentBehaviorState.value = bottomSheetBehavior.state

        fab.setOnClickListener {
            bottomSheetBehavior.state = if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                BottomSheetBehavior.STATE_HIDDEN
            } else {
                BottomSheetBehavior.STATE_EXPANDED
            }
        }

        fab.show()

        searchButton.setOnClickListener {
            val string = mainViewModel.searchString.value
            if (string.isNullOrBlank()) {
                val builder = AlertDialog.Builder(requireContext())
                val inflater = requireActivity().layoutInflater
                val dialogView = inflater.inflate(R.layout.search_dialog_view, null)
                dialogView.editText.setText(mainViewModel.searchString.value)
                builder.setPositiveButton(R.string.search_button_label) { dialog, _ ->
                    mainViewModel.searchString.value = dialogView.editText.text.toString()
                    dialog.cancel()
                }
                    .setNegativeButton(R.string.close_button_label) { dialog, _ ->
                        dialog.cancel()
                    }
                builder.setView(dialogView).show().apply {
                    val layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    layoutParams.marginStart = 10
                    layoutParams.marginEnd = 10
                    layoutParams.weight = 10F

                    val negativeButton = this.getButton(AlertDialog.BUTTON_NEGATIVE)
                    negativeButton.setTextColor(resources.getColor(R.color.justBlack, null))
                    negativeButton.background =
                        resources.getDrawable(R.color.yellowBackground, null)
                    negativeButton.layoutParams = layoutParams

                    val positiveButton = this.getButton(AlertDialog.BUTTON_POSITIVE)
                    positiveButton.setTextColor(resources.getColor(R.color.justBlack, null))
                    positiveButton.background =
                        resources.getDrawable(R.color.yellowBackground, null)
                    positiveButton.layoutParams = layoutParams
                }
            } else {
                mainViewModel.searchString.value = null
            }
        }
    }

    override fun onResume() {
        super.onResume()
        bottomSheetBehavior.state = currentBehaviorState.value ?: BottomSheetBehavior.STATE_HIDDEN
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
//            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            MainFragment()

        private const val BOTTOM_SHEET_BEHAVIOR_STATE = "bottom-sheet-behavior-state"
    }
}
