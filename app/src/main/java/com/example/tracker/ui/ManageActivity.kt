package com.example.tracker.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tracker.R
import com.example.tracker.app
import com.example.tracker.data.db.MedicineEntity
import com.example.tracker.data.db.MedicineSlotEntity
import com.example.tracker.data.db.PatientEntity
import com.example.tracker.databinding.ActivityManageBinding
import com.example.tracker.databinding.ItemManageMedicineBinding
import com.example.tracker.databinding.ItemManagePatientBinding
import com.example.tracker.databinding.ItemNoDosesBinding
import com.example.tracker.domain.AgeGroup
import com.example.tracker.domain.Gender
import com.example.tracker.domain.ScheduleRules
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Parent area: every patient with their medicines. Edit or delete a patient, open a medicine. */
class ManageActivity : AppCompatActivity() {

    private lateinit var b: ActivityManageBinding
    private val repository get() = app.repository
    private val adapter = ManageAdapter(
        onEditPatient = { p, age -> PatientDialogFragment.show(supportFragmentManager, p.id, p.name, age, Gender.fromStored(p.gender)) },
        onDeletePatient = ::confirmDeletePatient,
        onOpenMedicine = { m -> startActivity(MedicineEditActivity.editIntent(this, m.id)) },
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        b = ActivityManageBinding.inflate(layoutInflater)
        setContentView(b.root)
        ViewCompat.setOnApplyWindowInsetsListener(b.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }
        b.toolbar.setNavigationOnClickListener { finish() }
        b.list.adapter = adapter
        Responsive.limitWidth(b.list)

        supportFragmentManager.setFragmentResultListener(ConfirmDialogFragment.REQUEST_KEY, this) { _, r ->
            if (r.getString(ConfirmDialogFragment.RESULT_PURPOSE) == PURPOSE_DELETE_PATIENT &&
                r.getBoolean(ConfirmDialogFragment.RESULT_YES)
            ) {
                val id = r.getLong(ConfirmDialogFragment.RESULT_PAYLOAD)
                lifecycleScope.launch { app.alarms.cancel(repository.deletePatient(id)) }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(repository.observePatients(), repository.observeMedicines(), repository.observeSlots()) { p, m, s ->
                    Triple(p, m, s)
                }.collect { (patients, medicines, slots) -> render(patients, medicines, slots) }
            }
        }
    }

    private fun render(patients: List<PatientEntity>, medicines: List<MedicineEntity>, slots: List<MedicineSlotEntity>) {
        val slotsByMedicine = slots.groupBy { it.medicineId }
        val items = buildList {
            for (p in patients) {
                val age = repository.currentAge(p)
                add(ManageItem.Patient(p, age, AgeGroup.of(age), label(p, patients)))
                val own = medicines.filter { it.patientId == p.id }
                if (own.isEmpty()) add(ManageItem.NoMedicines(p.id))
                own.forEach { add(ManageItem.Medicine(it, summary(this@ManageActivity, it, slotsByMedicine[it.id].orEmpty()))) }
            }
        }
        adapter.submitList(items)
        b.empty.isVisible = patients.isEmpty()
    }

    private fun label(p: PatientEntity, all: List<PatientEntity>): String {
        val age = repository.currentAge(p)
        val twins = all.filter { it.name == p.name && repository.currentAge(it) == age }
        val name = Fmt.bidi(this, p.name)
        return if (twins.size <= 1) getString(R.string.patient_label, name, age)
        else getString(R.string.patient_label_numbered, name, age, twins.indexOfFirst { it.id == p.id } + 1)
    }

    private fun confirmDeletePatient(p: PatientEntity) {
        val name = Fmt.bidi(this, p.name)
        ConfirmDialogFragment.show(
            supportFragmentManager,
            PURPOSE_DELETE_PATIENT,
            title = getString(R.string.delete_patient_title, name),
            message = getString(R.string.delete_patient_message, name),
            positive = getString(R.string.delete),
            negative = getString(R.string.cancel),
            payload = p.id,
        )
    }

    companion object {
        private const val PURPOSE_DELETE_PATIENT = "delete_patient"

        /** Short human summary of a weekly schedule and its dates. */
        fun summary(ctx: Context, m: MedicineEntity, slots: List<MedicineSlotEntity>): String {
            val byDay = slots.groupBy { it.dayOfWeek }.mapValues { (_, s) -> s.map { it.minuteOfDay }.sorted() }
            val sets = byDay.values.toSet()
            val sep = ctx.getString(R.string.list_separator)
            val times = { minutes: List<Int> -> minutes.joinToString(sep) { Fmt.time(ctx, it) } }
            val schedule = when {
                slots.isEmpty() -> ctx.getString(R.string.summary_no_times)
                byDay.keys == ScheduleRules.ALL_DAYS.toSet() && sets.size == 1 ->
                    ctx.getString(R.string.summary_every_day, times(sets.first()))
                sets.size == 1 -> ctx.getString(
                    R.string.summary_days,
                    Fmt.weekOrder(ctx).filter { it in byDay.keys }.joinToString(sep) { Fmt.dayShort(ctx, it) },
                    times(sets.first()),
                )
                else -> ctx.getString(R.string.summary_custom)
            }
            val start = m.startEpochDay
            val end = m.endEpochDay
            val dates = when {
                start != null && end != null ->
                    ctx.getString(R.string.summary_from_to, Fmt.date(ctx, start), Fmt.date(ctx, end))
                start != null -> ctx.getString(R.string.summary_from, Fmt.date(ctx, start))
                end != null -> ctx.getString(R.string.summary_until, Fmt.date(ctx, end))
                else -> null
            }
            return if (dates == null) schedule else "$schedule\n$dates"
        }
    }
}

private sealed interface ManageItem {
    val key: String

    data class Patient(val patient: PatientEntity, val age: Int, val group: AgeGroup, val label: String) : ManageItem {
        override val key get() = "p${patient.id}"
    }

    data class Medicine(val medicine: MedicineEntity, val summary: String) : ManageItem {
        override val key get() = "m${medicine.id}"
    }

    data class NoMedicines(val patientId: Long) : ManageItem {
        override val key get() = "n$patientId"
    }
}

private class ManageAdapter(
    private val onEditPatient: (PatientEntity, Int) -> Unit,
    private val onDeletePatient: (PatientEntity) -> Unit,
    private val onOpenMedicine: (MedicineEntity) -> Unit,
) : ListAdapter<ManageItem, RecyclerView.ViewHolder>(object : DiffUtil.ItemCallback<ManageItem>() {
    override fun areItemsTheSame(old: ManageItem, new: ManageItem) = old.key == new.key
    override fun areContentsTheSame(old: ManageItem, new: ManageItem) = old == new
}) {
    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is ManageItem.Patient -> 0
        is ManageItem.Medicine -> 1
        is ManageItem.NoMedicines -> 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> object : RecyclerView.ViewHolder(ItemManagePatientBinding.inflate(inflater, parent, false).root) {}
            1 -> object : RecyclerView.ViewHolder(ItemManageMedicineBinding.inflate(inflater, parent, false).root) {}
            else -> object : RecyclerView.ViewHolder(ItemNoDosesBinding.inflate(inflater, parent, false).root) {}
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val ctx = holder.itemView.context
        when (val item = getItem(position)) {
            is ManageItem.Patient -> {
                val v = ItemManagePatientBinding.bind(holder.itemView)
                v.avatar.text = Mascots.avatar(item.group, Gender.fromStored(item.patient.gender))
                v.name.text = item.label
                v.group.text = Fmt.ageGroup(ctx, item.group)
                v.editButton.contentDescription = ctx.getString(R.string.edit_patient_named, item.patient.name)
                v.deleteButton.contentDescription = ctx.getString(R.string.delete_patient_named, item.patient.name)
                v.editButton.setOnClickListener { onEditPatient(item.patient, item.age) }
                v.deleteButton.setOnClickListener { onDeletePatient(item.patient) }
            }
            is ManageItem.Medicine -> {
                val v = ItemManageMedicineBinding.bind(holder.itemView)
                v.name.text = item.medicine.name
                v.schedule.text = item.summary
                v.root.contentDescription = ctx.getString(R.string.edit_medicine_named, item.medicine.name)
                v.root.setOnClickListener { onOpenMedicine(item.medicine) }
            }
            is ManageItem.NoMedicines -> ItemNoDosesBinding.bind(holder.itemView).text.setText(R.string.no_medicines_for_patient)
        }
    }
}
