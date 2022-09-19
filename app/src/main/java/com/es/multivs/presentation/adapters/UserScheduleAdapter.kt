package com.es.multivs.presentation.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.es.multivs.R
import com.es.multivs.data.TimeStampComparator

import com.es.multivs.data.models.ScheduleItem
import com.es.multivs.data.utils.ScheduleUtils
import java.util.*

class UserScheduleAdapter(
    private val context: Context,
    private var scheduleItems: MutableList<ScheduleItem>,
) :
    RecyclerView.Adapter<UserScheduleAdapter.ScheduleViewHolder>() {

    fun setListener(callback: ScheduleAdapterListener) {
        this.callback = callback
    }

    interface ScheduleAdapterListener {
        fun onCardClicked(item: ScheduleItem)
    }

    var callback: ScheduleAdapterListener? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): UserScheduleAdapter.ScheduleViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.schedule_layout, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val item = scheduleItems[position]
        holder.titleTv.text = item.getTitle()
        holder.timeStampTv.text = item.getTime()

        if (item.taskDone()) {

            holder.titleTv.alpha = 0.3f
            holder.timeStampTv.alpha = 0.3f
            holder.iconIv.alpha = 0.3f

            holder.checkIv.visibility = View.VISIBLE
        } else {
            holder.checkIv.visibility = View.INVISIBLE
            holder.titleTv.alpha = 1f
            holder.timeStampTv.alpha = 1f
            holder.iconIv.alpha = 1f
        }

        val scheduleTimeMillis = ScheduleUtils.timeStampToMillis(item.getTime())
        val currentTimeMillis = ScheduleUtils.getCurrentTimeOfDay()
        if (scheduleTimeMillis < currentTimeMillis && !item.isItemActive()) {
            holder.titleTv.alpha = 0.3f
            holder.timeStampTv.alpha = 0.3f
            holder.iconIv.alpha = 0.3f
        }

        if (item.isItemActive()) {
            setAnimation(holder)
            //TODO: motion layout?
        } else {
            holder.activeFrame.clearAnimation()
            holder.activeFrame.visibility = View.GONE
        }

        when (item.getItemType()) {
            context.getString(R.string.measurement_item) -> {
                holder.iconIv.setImageResource(R.drawable.ic_measurement)
            }
            context.getString(R.string.medication_item) -> {
                holder.iconIv.setImageResource(R.drawable.ic_medications)
            }
            context.getString(R.string.calibration_item) -> {
                holder.iconIv.setImageResource(R.drawable.ic_calibrations)
            }
            context.getString(R.string.survey_item)->{
                holder.iconIv.setImageResource(R.drawable.ic_survey)
            }
        }
    }

    private fun setAnimation(holder: UserScheduleAdapter.ScheduleViewHolder) {
        if (holder.isActive) {
            val blinkAnimation = AnimationUtils.loadAnimation(context, R.anim.blink)
            holder.activeFrame.visibility = View.VISIBLE
            holder.activeFrame.clearAnimation()
            holder.activeFrame.startAnimation(blinkAnimation)
        }
    }

    override fun onViewAttachedToWindow(holder: ScheduleViewHolder) {
        setAnimation(holder)
    }

    override fun getItemCount(): Int {
        return scheduleItems.size
    }

    /**
     * notifies active schedules.
     * @return index of the closest schedule to the current time
     */
    fun notifyActiveSchedules(): Int {
        var scrollToIndex = 0
        var delta = Long.MAX_VALUE
        scheduleItems.forEachIndexed { index, item ->
            if (item.isItemActive()) {
                notifyItemChanged(index)
                val thisDelta = ScheduleUtils.getTimeDifference(item)
                if (thisDelta < delta) {
                    delta = thisDelta
                    scrollToIndex = index
                }
            }
        }
        return scrollToIndex
    }

    fun addSchedules(items: List<ScheduleItem>) {

        scheduleItems = items.toMutableList()

        val str=""
        scheduleItems.forEach {
            if (it.taskDone()){
                str.plus(it.getItemType()).plus(" ${it.getTime()}")
            }
        }
        Collections.sort(scheduleItems, TimeStampComparator())
        notifyDataSetChanged()

    }

    fun clearSchedules() {
        scheduleItems.clear()
        notifyDataSetChanged()
    }

    inner class ScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var titleTv: TextView
        var timeStampTv: TextView
        var iconIv: ImageView
        var checkIv: ImageView
        var activeFrame: RelativeLayout
        var scheduleCard: CardView
        var isActive: Boolean = false

        init {
            itemView.setOnClickListener {
                callback?.onCardClicked(scheduleItems[adapterPosition])
            }

            itemView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(p0: View?) {
                    if (adapterPosition != -1)
                        isActive = scheduleItems[adapterPosition].isItemActive()
                }

                override fun onViewDetachedFromWindow(p0: View?) {

                }
            })

            titleTv = itemView.findViewById(R.id.schedule_title_tv)
            timeStampTv = itemView.findViewById(R.id.schedule_time_stamp_tv)
            iconIv = itemView.findViewById(R.id.schedule_icon_iv)
            activeFrame = itemView.findViewById(R.id.active_schedule)
            checkIv = itemView.findViewById(R.id.check_iv)
            scheduleCard = itemView.findViewById(R.id.schedule_card)
        }
    }
}