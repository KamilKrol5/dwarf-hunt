package com.example.krasnalhunt

import android.graphics.drawable.ClipDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.krasnalhunt.model.AppDatabase
import com.example.krasnalhunt.model.DwarfItem


class DwarfItemListFragment : Fragment(), MyDwarfItemRecyclerViewAdapter.OnListFragmentInteractionListener {

    override fun onListFragmentInteraction(item: DwarfItem?) {
        Log.d("TAG", "$item clicked")
        Toast.makeText(context, "${item?.name}", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
    }

    private lateinit var mAdapter: MyDwarfItemRecyclerViewAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dwarfitem_list, container, false)

        if (view is RecyclerView) {
            with(view) {
                layoutManager = LinearLayoutManager(context)
                mAdapter = MyDwarfItemRecyclerViewAdapter(this@DwarfItemListFragment)
                adapter = mAdapter
                // TODO: decide whether setHasFixedSize(true) is correct
            }
            val decoration = DividerItemDecoration(context, ClipDrawable.HORIZONTAL)
            view.addItemDecoration(decoration)
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        AppDatabase.instance?.let { db ->
            db.dwarfItemDao()
                .findItems()
                .observe(this, Observer { newList ->
                    mAdapter.updateData(newList)
                })
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            DwarfItemListFragment()
    }
}
