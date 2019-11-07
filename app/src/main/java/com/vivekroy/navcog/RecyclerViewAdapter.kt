import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vivekroy.navcog.Beacon
import com.vivekroy.navcog.R

//class VvkAdapter(private val activity: Activity, var items : List<Beacon>) : BaseAdapter() {
//    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
//        var v = convertView
//        if (v == null) {
//            v = activity.layoutInflater.inflate(R.layout.listitem, parent, false)
//        }
//        v!!.findViewById<TextView>(R.id.rssi).text = items[position].rssi.toString()
//        v.findViewById<TextView>(R.id.major).text = items[position].major.toString()
//        v.findViewById<TextView>(R.id.minor).text = items[position].minor.toString()
//        return v
//    }
//
//    override fun getItem(position: Int): Any {
//        return items[position]
//    }
//
//    override fun getItemId(position: Int): Long {
//        return items[position].major*1000L + items[position].minor
//    }
//
//    override fun getCount(): Int {
//        return items.size
//    }
//
//}

class VvkAdapter() : ListAdapter<Beacon, VvkAdapter.VvkViewHolder>(Beacon.DIFF_CB) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VvkViewHolder {
        return VvkViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.listitem, parent, false))
    }

    override fun onBindViewHolder(holder: VvkViewHolder, position: Int) {
        val b = getItem(position)
        if (b != null)
            holder.bindTo(b)
    }

    data class VvkViewHolder(val v : View) : RecyclerView.ViewHolder(v) {
        val rssi = v.findViewById<TextView>(R.id.rssi)
        val major = v.findViewById<TextView>(R.id.major)
        val minor = v.findViewById<TextView>(R.id.minor)

        fun bindTo(b : Beacon) {
            rssi.text = b.rssi.toString()
            major.text = b.major.toString()
            minor.text = b.minor.toString()
        }
    }

}