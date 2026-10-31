package com.example.tracker.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tracker.R
import com.example.tracker.data.HomePatient
import com.example.tracker.data.db.DoseRow
import com.example.tracker.databinding.ItemDoseBinding
import com.example.tracker.databinding.ItemNoDosesBinding
import com.example.tracker.databinding.ItemPatientBlockBinding
import com.example.tracker.domain.DoseStatus
import com.example.tracker.domain.Gender
import com.google.android.material.color.MaterialColors

/**
 * One block per patient: the section header (when "All" is shown) and that patient's doses. Whole
 * blocks are the grid cells, so on wide screens two patients sit side by side (CLAUDE.md 9).
 */
data class HomeBlock(
    val patient: HomePatient,
    /** Null when a single patient is selected: the top header already shows them. */
    val label: String?,
    val doses: List<Dose>,
) {
    val key get() = patient.patient.id

    data class Dose(val row: DoseRow, val patientName: String, val overdue: Boolean)
}

class HomeAdapter(
    private val onConfirm: (DoseRow, String) -> Unit,
    private val onEdit: (DoseRow) -> Unit,
) : ListAdapter<HomeBlock, HomeAdapter.BlockHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        BlockHolder(ItemPatientBlockBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: BlockHolder, position: Int) = holder.bind(getItem(position))

    inner class BlockHolder(private val b: ItemPatientBlockBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(block: HomeBlock) {
            val ctx = b.root.context
            val p = block.patient
            b.header.root.isVisible = block.label != null
            if (block.label != null) {
                b.header.avatar.text = Mascots.avatar(p.ageGroup, Gender.fromStored(p.patient.gender))
                b.header.name.text = block.label
                b.header.stats.text = ctx.getString(
                    R.string.section_stats,
                    Fmt.points(ctx, p.totalPoints),
                    Fmt.days(ctx, p.liveStreak),
                    p.level,
                )
                // With no doses the "no pills today" card below already says it.
                b.header.progress.isVisible = p.doses.isNotEmpty()
                b.header.progress.text = statusText(ctx, p)
            }
            val inflater = LayoutInflater.from(ctx)
            b.doses.removeAllViews()
            if (block.doses.isEmpty()) {
                ItemNoDosesBinding.inflate(inflater, b.doses, true).text.setText(R.string.no_doses_today)
            }
            block.doses.forEach { bindDose(ItemDoseBinding.inflate(inflater, b.doses, true), it) }
        }

        private fun bindDose(d: ItemDoseBinding, item: HomeBlock.Dose) {
            val ctx = d.root.context
            val row = item.row
            val status = DoseStatus.valueOf(row.status)
            d.medicine.text = row.medicineName
            d.time.text = Fmt.time(ctx, row.scheduledMinuteOfDay)
            d.icon.text = when (status) {
                DoseStatus.TAKEN -> Mascots.TAKEN
                DoseStatus.MISSED -> Mascots.MISSED
                DoseStatus.PENDING -> Mascots.PILL
            }
            d.takeButton.isVisible = status == DoseStatus.PENDING
            d.statusLabel.isVisible = status != DoseStatus.PENDING
            d.statusLabel.text = ctx.getString(if (status == DoseStatus.TAKEN) R.string.dose_taken else R.string.dose_missed)
            d.statusLabel.setTextColor(
                if (status == DoseStatus.TAKEN) ctx.getColor(R.color.success)
                else MaterialColors.getColor(d.statusLabel, androidx.appcompat.R.attr.colorError)
            )
            if (Responsive.narrowCards(d.root)) {
                // Not enough width for name and button side by side: the button gets its own line.
                d.row.removeView(d.actions)
                d.actionsBelow.addView(d.actions, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                d.takeButton.updateLayoutParams<FrameLayout.LayoutParams> { width = MATCH_PARENT }
                d.actionsBelow.isVisible = true
            }
            d.hint.isVisible = item.overdue
            d.hint.text = ctx.getString(R.string.dose_overdue)
            d.takeButton.contentDescription = ctx.getString(R.string.confirm_button_for, item.patientName, row.medicineName)
            d.editButton.contentDescription = ctx.getString(R.string.edit_medicine_named, row.medicineName)
            Motion.springPress(d.takeButton)
            d.takeButton.setOnClickListener { onConfirm(row, item.patientName) }
            d.editButton.setOnClickListener { onEdit(row) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<HomeBlock>() {
        override fun areItemsTheSame(old: HomeBlock, new: HomeBlock) = old.key == new.key
        override fun areContentsTheSame(old: HomeBlock, new: HomeBlock) = old == new
    }

    companion object {
        /** Derived from current doses, never a stored flag. */
        fun statusText(ctx: android.content.Context, p: HomePatient): String = when {
            p.doses.isEmpty() -> ctx.getString(R.string.status_no_doses)
            p.todayComplete -> ctx.getString(R.string.status_perfect_day)
            else -> ctx.getString(R.string.status_progress, p.takenToday, p.doses.size)
        }
    }
}
