package io.logbase.cakebeedelivery;

/**
 * Created by logbase on 20/11/15.
 */
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.LinearLayout;

import java.text.SimpleDateFormat;
import java.util.List;
import android.graphics.Color;

import com.firebase.client.Firebase;

public class OrderlistAdapter extends ArrayAdapter<OrderDetails> {
    private final Context context;
    private final List<OrderDetails> values;
    private final Boolean hasStartedOrder;

    public OrderlistAdapter(Context context, List<OrderDetails> values, boolean hasStartedOrder) {
        super(context, R.layout.orders, values);
        this.context = context;
        this.values = values;
        this.hasStartedOrder = hasStartedOrder;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.order_template, parent, false);
        TextView textView1 = (TextView) rowView.findViewById(R.id.label1);
        TextView textView2 = (TextView) rowView.findViewById(R.id.label2);
        TextView textView3 = (TextView) rowView.findViewById(R.id.label3);
        TextView textView4 = (TextView) rowView.findViewById(R.id.label4);

        Button startbtn = (Button)rowView.findViewById(R.id.startbtn);
        startbtn.getBackground().setColorFilter(0xFF00b5ad, PorterDuff.Mode.MULTIPLY);
        startbtn.setTag(position);
        startbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int position = Integer.parseInt(v.getTag().toString());
                OrderDetails ord = values.get(position);

                SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE);
                String deviceID = sharedPref.getString("deviceID", null);
                String accountID = sharedPref.getString("accountID", null);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                String currentDate = sdf.format(new java.util.Date());
                sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                String currentdateandtime = sdf.format(new java.util.Date());

                Firebase myFirebaseRef = new Firebase(context.getString(R.string.friebaseurl)+"accounts/"+accountID+"/orders/"+deviceID+"/"+currentDate+"/"+ord.Id+"/Startedon");
                myFirebaseRef.setValue(currentdateandtime);

                Utility.sendActivity(accountID, deviceID, ord.Id, "STARTED", sharedPref);
            }
        });

        OrderDetails orderDetail = values.get(position);
        if(this.hasStartedOrder == false && orderDetail.Status == "Picked up" && (orderDetail.Startedon == null || orderDetail.Startedon == "")) {
            startbtn.setVisibility(View.VISIBLE);
        }
        else {
            startbtn.setVisibility(View.GONE);
        }

        textView1.setText("#"+orderDetail.Id+ " ("+ orderDetail.Time+")");
        textView2.setText(orderDetail.Name);
        textView3.setText(orderDetail.Address);
        textView4.setText((orderDetail.Status == "Picked up" && orderDetail.Startedon != null && orderDetail.Startedon != "") ? "Delivering this order" : orderDetail.Status);

        textView4.setTextColor(Color.parseColor(getColorCode(orderDetail)));
        return rowView;
    }

    private String getColorCode(OrderDetails orderDetail) {
        if(orderDetail.Status == "Cancelled") {
            return "#a9a9a9";
        }
        else if(orderDetail.Status == "Yet to pick") {
            return "#7986CB";
        }
        else if(orderDetail.Status == "Picked up" && orderDetail.Startedon != null && orderDetail.Startedon != "") {
            return "#B40431";
        }
        else if(orderDetail.Status == "Picked up") {
            return "#BA68C8";
        }
        else if(orderDetail.Status == "Delivered") {
            return "#66BB6A";
        }
        else {
            return "#FF9800";
        }
    }
}
