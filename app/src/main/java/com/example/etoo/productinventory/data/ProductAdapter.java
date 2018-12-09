package com.example.etoo.productinventory.data;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.example.etoo.productinventory.EditorActivity;
import com.example.etoo.productinventory.InventoryActivity;
import com.example.etoo.productinventory.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    ArrayList<Product> objects;
    private LayoutInflater mInflater;
    private FirebaseDatabase mFirebaseDatabase;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    public ProductAdapter(@NonNull Context context, @NonNull ArrayList<Product> objects) {
        this.objects = objects;
        mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View mView = mInflater.inflate(R.layout.inventory_item, viewGroup, false);
        return new ProductViewHolder(mView, this);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder productViewHolder, int position) {
        Product mCurrent = objects.get(position);
        if (mCurrent != null) {
            productViewHolder.itemName.setText(mCurrent.getProductName());
            productViewHolder.sellingPrice.setText(mCurrent.getSellingPrice());
            productViewHolder.itemQuantity.setText(mCurrent.getProductQuantity());

            if (mCurrent.getImage() != null) {
                if (!mCurrent.getImage().equals("")) {
                    Picasso.get().load(mCurrent.getImage()).error(R.drawable.noimage).into(productViewHolder.listItemImage);
                }
            } else {
                productViewHolder.listItemImage.setImageResource(R.drawable.noimage);
            }
        }
    }

    @Override
    public int getItemCount() {
        return objects.size();
    }

    public void filterList(ArrayList<Product> filteredList) {
        objects = filteredList;
        notifyDataSetChanged();
    }

    class ProductViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public final ImageView listItemImage;
        public final TextView sellingPrice, itemQuantity, itemName;
        public final LinearLayout listItem;
        final ProductAdapter mAdapter;
        private DatabaseReference myRef;
        private String userID;
        private StorageReference mStorageRef;


        public ProductViewHolder(@NonNull final View itemView, ProductAdapter adapter) {
            super(itemView);
            listItem = itemView.findViewById(R.id.listItem);
            listItemImage = itemView.findViewById(R.id.listImage);
            sellingPrice = itemView.findViewById(R.id.priceView);
            itemQuantity = itemView.findViewById(R.id.quantityView);
            itemName = itemView.findViewById(R.id.nameView);

            mAdapter = adapter;

            mStorageRef = FirebaseStorage.getInstance().getReference();
            //this will hold pur collection of products
            mAuth = FirebaseAuth.getInstance();
            mFirebaseDatabase = FirebaseDatabase.getInstance();
            myRef = mFirebaseDatabase.getReference();
            final FirebaseUser user = mAuth.getCurrentUser();
            userID = user.getUid();

            listItem.setOnClickListener(this);
            listItem.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(final View v) {
                    final Product prd = (Product) objects.get(getLayoutPosition());

                    // Create an AlertDialog.Builder and set the message, and click listeners
                    // for the postivie and negative buttons on the dialog.

                    AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());

                    builder.setMessage(R.string.delete_dialog_msg);
                    builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, final int id) {
                            if (!prd.getProductGroup().equals("")) {
                                // User clicked the "Delete" button, so delete the product.
                                myRef.child(userID).child("Products").child(prd.getProductGroup()).child(prd.getProductBarcode()).setValue(null);
                                StorageReference userImages = mStorageRef.child(userID).child(prd.getProductBarcode());
                                userImages.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Toast.makeText(v.getContext(), "Prd image also deleted", Toast.LENGTH_SHORT).show();
                                    }
                                });

                            } else {
                                myRef.child(userID).child("Products").child(EditorActivity.NOT_GROUPED).child(prd.getProductBarcode()).setValue(null);
                                StorageReference userImages = mStorageRef.child(userID).child(prd.getProductBarcode());
                                userImages.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Toast.makeText(v.getContext(), "Prd image also deleted", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                            Toast.makeText(v.getContext(), "Product has been deleted.", Toast.LENGTH_SHORT).show();
                        }
                    });
                    builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User clicked the "Cancel" button, so dismiss the dialog
                            // and continue editing the product.

                            dialog.dismiss();

                        }
                    });
                    // Create and show the AlertDialog
                    AlertDialog alertDialog = builder.create();
                    //   alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                    alertDialog.show();
                    return true;
                }
            });

        }

        @Override
        public void onClick(View v) {
            Product prd = objects.get(getLayoutPosition());
            Context context = v.getContext();
            Intent intent = new Intent(context, EditorActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            intent.putExtra("prdName", prd.getProductName());
            intent.putExtra("prdSellingPrice", prd.getSellingPrice());
            intent.putExtra("prdPurchasePrice", prd.getPurchasePrice());
            intent.putExtra("prdQuantity", prd.getProductQuantity());
            intent.putExtra("prdSupplierName", prd.getProductSupplier());
            intent.putExtra("prdSupplierPhone", prd.getProductSupplierPhone());
            intent.putExtra("prdBarcode", prd.getProductBarcode());
            intent.putExtra("prdImageUrl", prd.getImage());
            intent.putExtra("prdGroupName", prd.getProductGroup());
            intent.putExtra("update", "update");
            context.startActivity(intent);
            EditorActivity.setEditPage(true);
        }

    }
}
