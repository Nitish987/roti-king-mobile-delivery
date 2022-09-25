package com.rotiking.delivery.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.rotiking.delivery.OrderDetailActivity;
import com.rotiking.delivery.R;
import com.rotiking.delivery.models.Order;
import com.rotiking.delivery.utils.DateParser;

import java.util.Date;

public class OrderRecyclerAdapter extends FirestoreRecyclerAdapter<Order, OrderRecyclerAdapter.OrderHolder> {
    private final LinearLayout noOrdersI;

    public OrderRecyclerAdapter(@NonNull FirestoreRecyclerOptions<Order> options, LinearLayout noOrdersI) {
        super(options);
        this.noOrdersI = noOrdersI;
    }

    @Override
    protected void onBindViewHolder(@NonNull OrderRecyclerAdapter.OrderHolder holder, int position, @NonNull Order model) {
        noOrdersI.setVisibility(View.INVISIBLE);

        holder.setOrderNumber(model.getOrderNumber());
        holder.setItems(model.getItems().size());
        holder.setPaymentMethod(model.getPaymentMethod());
        holder.setStatus(model.getOrderState(), model.isOrderSuccess());
        holder.setTime(model.getTime());
        holder.setPayableAmt(model.getPayablePrice());

        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(view.getContext(), OrderDetailActivity.class);
            intent.putExtra("ORDER", model.getOrderId());
            view.getContext().startActivity(intent);
        });
    }

    @NonNull
    @Override
    public OrderRecyclerAdapter.OrderHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new OrderHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false));
    }

    public static class OrderHolder extends RecyclerView.ViewHolder {
        private final TextView orderNumber, items, paymentMethod, status, time, payableAmt;

        public OrderHolder(@NonNull View itemView) {
            super(itemView);
            orderNumber = itemView.findViewById(R.id.order_number);
            items = itemView.findViewById(R.id.items);
            paymentMethod = itemView.findViewById(R.id.payment_method);
            status = itemView.findViewById(R.id.status);
            time = itemView.findViewById(R.id.time);
            payableAmt = itemView.findViewById(R.id.payable_amt);
        }

        public void setOrderNumber(int orderNumber) {
            String st = "#~order~" + orderNumber;
            this.orderNumber.setText(st);
        }

        public void setItems(int count) {
            String st = count + " Food Items";
            this.items.setText(st);
        }

        public void setPaymentMethod(String paymentMethod) {
            String st = "Payment Method : " + paymentMethod;
            this.paymentMethod.setText(st);
        }

        public void setStatus(int state, boolean isOrderSuccess) {
            String st = "";
            if (isOrderSuccess) {
                switch (state) {
                    case 0:
                        st = "Ordered...";
                        break;
                    case 1:
                        st = "Cooking...";
                        break;
                    case 2:
                        st = "Dispatched...";
                        break;
                    case 3:
                        st = "On way...";
                        break;
                    case 4:
                        st = "Delivered...";
                        break;
                }
                this.status.setTextColor(itemView.getContext().getColor(R.color.green));
            } else {
                st = "Order was canceled.";
                this.status.setTextColor(itemView.getContext().getColor(R.color.red));
            }
            this.status.setText(st);
        }

        public void setTime(long time) {
            String st = DateParser.parse(new Date(time));
            this.time.setText(st);
        }

        public void setPayableAmt(int amt) {
            String st = "\u20B9 " + amt;
            this.payableAmt.setText(st);
        }
    }
}

