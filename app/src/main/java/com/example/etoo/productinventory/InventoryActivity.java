package com.example.etoo.productinventory;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.etoo.productinventory.data.Product;
import com.example.etoo.productinventory.data.ProductAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.util.ArrayList;



public class InventoryActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    private static final String TAG = "ViewDatabase";

    private FirebaseDatabase mFirebaseDatabase;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference myRef;
    private String userID;
    private ProgressBar mProgresBar;
    private FloatingActionButton createGroup;
    private FloatingActionButton createItem;
    private boolean isFABOpen;
    private ArrayList<Product> products;
    private ProductAdapter mAdapter;

    @Override
    public void onBackPressed() {
        if (!isFABOpen) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Exit from app")
                    .setMessage("Are you sure you want to exit ?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }

                    })
                    .setNegativeButton("No", null)
                    .show();
        } else closeFABMenu();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inventory);

        final View emptyList = findViewById(R.id.empty_view);

        //this will hold pur collection of products
        mAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        myRef = mFirebaseDatabase.getReference();
        final FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userID = user.getUid();
        }

        final RecyclerView productListView = findViewById(R.id.productList);

        mProgresBar = findViewById(R.id.progressBar);
        myRef.child(userID).child("Products").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                products = new ArrayList<>();

                int counter = 0;
                //Get all of the children at this level
                Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                //Veriyi alıp Product objesinde kaydediyor. Şu an için log ekranına yazdırabiliyor.
                //ListViewa yazdırmak gerekiyor.
                for (DataSnapshot child : children) {
                    Iterable<DataSnapshot> cocuklar = child.getChildren();
                    for (DataSnapshot cocuk : cocuklar) {
                        counter += 1;
                        Product prd = cocuk.getValue(Product.class);
                        if(!prd.getProductBarcode().equals("0x0x00x0xx"))
                        products.add(prd);
                        mProgresBar.setVisibility(View.VISIBLE);
                    }
                }
                mAdapter = new ProductAdapter(InventoryActivity.this, products);
                RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(InventoryActivity.this);
                productListView.setLayoutManager(layoutManager);
                productListView.setItemAnimator(new DefaultItemAnimator());
                productListView.setAdapter(mAdapter);
                if (counter == 0) {
                    emptyList.setVisibility(View.VISIBLE);
                } else {
                    emptyList.setVisibility(View.INVISIBLE);
                }
                mProgresBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());

                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                    toastMessage("Successfully signed out.");
                }
            }
        };

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = findViewById(R.id.fab);
        createGroup = findViewById(R.id.groupFab);
        createItem = findViewById(R.id.itemFab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isFABOpen) {
                    showFABMenu();
                } else {
                    closeFABMenu();
                }
            }
        });

        createGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                showGroupDialog();
                closeFABMenu();
            }
        });

        createItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(InventoryActivity.this, EditorActivity.class);
                startActivity(intent);
                EditorActivity.setEditPage(false);
                closeFABMenu();
            }
        });

    }

    private void filter(String text) {
       ArrayList<Product> filteredList = new ArrayList<Product>();

        for (Product item : products) {
            if (item.getProductName().toLowerCase().contains(text.toLowerCase())){
                filteredList.add(item);
            }
        }

        mAdapter.filterList(filteredList);

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem searchItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.search) {
            toastMessage("Search Clicked");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showFABMenu() {
        isFABOpen = true;
        createGroup.animate().translationY(-getResources().getDimension(R.dimen.standard_65));
        createItem.animate().translationY(-getResources().getDimension(R.dimen.standard_125));
    }

    private void closeFABMenu() {
        isFABOpen = false;
        createItem.animate().translationY(0);
        createGroup.animate().translationY(0);
    }

    private void showGroupDialog() {
        final AlertDialog.Builder mBuilder = new AlertDialog.Builder(InventoryActivity.this);
        final View dialogView = this.getLayoutInflater().inflate(R.layout.create_group, null);

        mBuilder.setView(dialogView);
        final AlertDialog dialog = mBuilder.create();

        final EditText groupName = dialogView.findViewById(R.id.groupNameEt);

        Button submitGroup = dialogView.findViewById(R.id.submitGroup);
        Button cancelGroup = dialogView.findViewById(R.id.cancelGroup);

        //Ürünlerin grubunu değişince eğer içinde hiç child kalmamışsa grup siliniyor.


        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        cancelGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        submitGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                myRef.child(userID).child("Group Names").child(groupName.getText().toString()).setValue(true);
                dialog.dismiss();
            }
        });

        dialog.show();
    }


    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }


    /**
     * customizable toast
     *
     * @param message
     */
    private void toastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

   @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        filter(newText);
        return false;
    }
}

