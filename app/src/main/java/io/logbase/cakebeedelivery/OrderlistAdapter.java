package io.logbase.cakebeedelivery;

/**
 * Created by logbase on 20/11/15.
 */
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import java.util.List;
import android.graphics.Color;

public class OrderlistAdapter extends ArrayAdapter<OrderDetails> {
    private final Context context;
    private final List<OrderDetails> values;

    public OrderlistAdapter(Context context, List<OrderDetails> values) {
        super(context, R.layout.orders, values);
        this.context = context;
        this.values = values;
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

        OrderDetails orderDetail = values.get(position);

        textView1.setText("#"+orderDetail.Id+ " ("+ orderDetail.Time+")");
        textView2.setText(orderDetail.Name);
        textView3.setText(orderDetail.Address);
        textView4.setText(orderDetail.Status);
        textView4.setBackgroundColor(Color.parseColor(getColorCode(orderDetail)));
        return rowView;
    }

    private String getColorCode(OrderDetails orderDetail) {
        if(orderDetail.Status == "Yet to pick") {
            return "#7986CB";
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
