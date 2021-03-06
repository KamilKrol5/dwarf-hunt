package com.example.krasnalhunt

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.krasnalhunt.model.DwarfItem
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_dwarfitem.view.*

class MyDwarfItemRecyclerViewAdapter(
    private val mListener: OnListFragmentInteractionListener?
) : RecyclerView.Adapter<MyDwarfItemRecyclerViewAdapter.ViewHolder>() {

    interface OnListFragmentInteractionListener {
        fun onListFragmentInteraction(item: Pair<DwarfItem, Float>)
    }

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as Pair<DwarfItem, Float>
            mListener?.onListFragmentInteraction(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_dwarfitem, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = ald.currentList[position]
        val dwarf = item.first
        holder.nameHolder.text = dwarf.name
        holder.locationHolder.text = dwarf.location
        val imgResId = holder.imageHolder.resources.getIdentifier(
            dwarf.fileName.dropLast(4),
            "drawable",
            holder.imageHolder.context.packageName
        )
        if (imgResId == 0) {
            Picasso.get().load(R.drawable.no_image).into(holder.imageHolder)
        } else {
            Picasso.get().load(imgResId).error(R.drawable.no_image).fit().into(holder.imageHolder)
        }
        val distance = item.second.toInt()
        if (dwarf.caught) {
            holder.caughtImageHolder.setImageResource(R.drawable.ic_check_icon)
            holder.caughtImageBackground.background =
                holder.caughtImageBackground.resources.getDrawable(R.color.greenBackground, null)
        } else if (distance < 50) {
            holder.caughtImageHolder.setImageResource(R.drawable.ic_hand_yellow)
            holder.caughtImageBackground.background =
                holder.caughtImageBackground.resources.getDrawable(R.color.yellowBackground, null)
        } else {
            holder.caughtImageHolder.setImageResource(R.drawable.ic_check_icon_gray)
            holder.caughtImageBackground.background =
                holder.caughtImageBackground.resources.getDrawable(R.color.grayBackground, null)
        }
        holder.distanceHolder.text = "$distance m"

        with(holder.mView) {
            tag = item
            setOnClickListener(mOnClickListener)
        }
    }

    private val ald = AsyncListDiffer<Pair<DwarfItem, Float>>(this, object : DiffUtil.ItemCallback<Pair<DwarfItem, Float>>() {
        override fun areContentsTheSame(oldItem: Pair<DwarfItem, Float>, newItem: Pair<DwarfItem, Float>): Boolean {
            return oldItem == newItem
        }

        override fun areItemsTheSame(oldItem: Pair<DwarfItem, Float>, newItem: Pair<DwarfItem, Float>): Boolean {
            return oldItem.first.id == newItem.first.id
        }

    })

    fun updateData(newItems: List<Pair<DwarfItem, Float>>) {
        ald.submitList(newItems)
    }

    override fun getItemCount(): Int = ald.currentList.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val nameHolder: TextView = mView.nameTextView
        val locationHolder: TextView = mView.locationTextView
        val imageHolder: ImageView = mView.dwarfImage
        val caughtImageHolder: ImageView = mView.caughtImage
        val caughtImageBackground: ImageView = mView.imageBackground
        val distanceHolder: TextView = mView.distanceTextView

        override fun toString(): String {
            return super.toString() + " '" + nameHolder.text + "'"
        }
    }
}
