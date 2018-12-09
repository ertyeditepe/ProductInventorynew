package com.example.etoo.productinventory;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.etoo.productinventory.data.Product;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.zxing.Result;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class EditorActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    private static final int GALLERY_REQUEST = 1;
    private static final int CAMERA_REQUEST_CODE = 2;
    private static final String TAG = "Editor Activity";
    public static String NOT_GROUPED = "Ungrouped";
    private static boolean editPage = false;
    String ifPhotoIsNotChangedImageUrl = "";
    String beforeChangedGroupName;
    String beforeBarcodeChanged;
    private boolean editTextsHaveChanged = false, isGroupChanged = false, isBarcodeChanged = false;
    private EditText prdName, prdSellingPrice, prdQuantity, prdSupplier, prdSupplierPhone, prdPurchasingPrice, prdBarcode;
    private ImageView prdImage;
    private DatabaseReference myRef;
    private String userID;
    private StorageReference mStorageRef;
    private Uri mImageUri = null;
    private ProgressBar editPageImageProgressBar;
    private EditText prdGroupName;
    private Spinner groupListSpinner;

    /**
     * Content URI for the existing product (null if it's a new product)
     */

    public static void setEditPage(boolean editPage) {
        EditorActivity.editPage = editPage;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.edit_page);
        editTextFind();
        ImageButton galeri = findViewById(R.id.galeriButton);
        ImageButton camera = findViewById(R.id.cameraButton);
        prdImage = findViewById(R.id.editPageImage);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();
        myRef = mFirebaseDatabase.getReference();
        final FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            userID = user.getUid();
        }

        //When activity started get barcode so if barcode has changed before saving product old barcoded product can be deleted
        beforeBarcodeChanged = prdBarcode.getText().toString();

        mStorageRef = FirebaseStorage.getInstance().getReference();

        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!prdBarcode.getText().toString().equals("")) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, CAMERA_REQUEST_CODE);
                } else {
                    toastMessage("Please enter the barcode for uploading an image");
                }
            }
        });

        galeri.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!prdBarcode.getText().toString().equals("")) {

                    Log.d(TAG, "onClick: accessing phones memory.");
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    startActivityForResult(intent, GALLERY_REQUEST);
                } else {
                    toastMessage("Please enter the barcode for uploading an image");
                }
            }
        });

        //Creates spinner for seeing the group list
        myRef.child(userID).child("Group Names").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Is better to use a List, because you don't know the size
                // of the iterator returned by dataSnapshot.getChildren() to
                // initialize the array
                final ArrayList<String> grupList = new ArrayList<>();
                grupList.add(0, "Change Group");
                Iterable<DataSnapshot> groups = dataSnapshot.getChildren();

                for (DataSnapshot group : groups) {
                    String areaName = group.getKey();
                    grupList.add(areaName);
                }

                groupListSpinner = findViewById(R.id.grupSpinner);
                ArrayAdapter<String> areasAdapter = new ArrayAdapter<String>(EditorActivity.this,
                        android.R.layout.simple_spinner_item, grupList) {
                    @Override
                    public boolean isEnabled(int position) {
                        if(position == 0) return false;
                        else return true;
                    }

                    @Override
                    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                        View view = super.getDropDownView(position, convertView, parent);
                        TextView tv = (TextView) view;
                        if (position == 0) {
                            // Set the hint text color gray
                            tv.setTextColor(Color.GRAY);
                        } else {
                            tv.setTextColor(Color.BLACK);
                        }
                        return view;
                    }
                };


                areasAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                groupListSpinner.setAdapter(areasAdapter);


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        groupListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedGroup = parent.getItemAtPosition(position).toString();
                int pos = parent.getSelectedItemPosition();
                if (pos != 0)
                    prdGroupName.setText(selectedGroup);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        if (editPage) {
            getEditProduct();
        }

        isFormChanged();


    }

    private void editTextFind() {
        prdName = findViewById(R.id.prdName);
        prdSellingPrice = findViewById(R.id.prdSellingPrice);
        prdPurchasingPrice = findViewById(R.id.prdPurchasePrice);
        prdBarcode = findViewById(R.id.barCodeEditText);
        prdQuantity = findViewById(R.id.prdQuantity);
        prdImage = findViewById(R.id.editPageImage);
        prdSupplier = findViewById(R.id.prdSupplier);
        prdSupplierPhone = findViewById(R.id.supplierPhone);
        prdGroupName = findViewById(R.id.prdGroupName);
        groupListSpinner = findViewById(R.id.grupSpinner);
    }

    private void updateExistingProduct() {

        editTextFind();

        Product newPrd = new Product();
        newPrd.setProductBarcode(prdBarcode.getText().toString());
        newPrd.setProductName(prdName.getText().toString());
        newPrd.setProductQuantity(prdQuantity.getText().toString());
        newPrd.setPurchasePrice(prdPurchasingPrice.getText().toString());
        newPrd.setSellingPrice(prdSellingPrice.getText().toString());
        newPrd.setProductSupplier(prdSupplier.getText().toString());
        newPrd.setProductSupplierPhone(prdSupplierPhone.getText().toString());
        if (prdGroupName.getText().toString().equals(""))
            newPrd.setProductGroup(NOT_GROUPED);
        else newPrd.setProductGroup(prdGroupName.getText().toString());
        //If Image uri is not null gets the image uri and set it as variable of object so I can know which image
        // in the storage for which product
        if (mImageUri != null) {
            newPrd.setImage(mImageUri.toString());
        } else {
            newPrd.setImage(ifPhotoIsNotChangedImageUrl);
        }

        //If product name and barcode is not null then product can be saved
        if (!newPrd.getProductName().equals("") && !newPrd.getProductBarcode().equals("")) {

            //If product doesn't have a group name save to the "Ungrouped" products list
            if (newPrd.getProductGroup().equals("")) {

                /* @beforeChangedGroupName activity başladığında ürünün adını alıyor
                    eğer activity başladığında grup ismi varsa ama save butonu tıklandığında
*/
                if (beforeChangedGroupName != null)
                    myRef.child(userID).child("Products").child(beforeChangedGroupName).child(newPrd.getProductBarcode()).setValue(null);

                if (isBarcodeChanged)
                    myRef.child(userID).child("Products").child(NOT_GROUPED).child(beforeBarcodeChanged).setValue(null);


                myRef.child(userID).child("Products").child(NOT_GROUPED).child(newPrd.getProductBarcode()).setValue(newPrd);

            } else {
                if (beforeChangedGroupName == null) {
                    myRef.child(userID).child("Products").child(NOT_GROUPED).child(newPrd.getProductBarcode()).setValue(null);
                } else {
                    if (!beforeChangedGroupName.equals(newPrd.getProductGroup())) {
                        myRef.child(userID).child("Group Names").child(newPrd.getProductGroup()).setValue(true);
                    }
                    // BURADA DEĞİŞİKLİK
                    myRef.child(userID).child("Products").child(beforeChangedGroupName).child(newPrd.getProductBarcode()).setValue(null);
                }
                if (isBarcodeChanged) {
                    myRef.child(userID).child("Products").child(newPrd.getProductGroup()).child(beforeBarcodeChanged).setValue(null);
                }
                if (isBarcodeChanged && isGroupChanged) {
                    myRef.child(userID).child("Products").child(beforeChangedGroupName).child(beforeBarcodeChanged).setValue(null);
                }
                myRef.child(userID).child("Products").child(newPrd.getProductGroup()).child(newPrd.getProductBarcode()).setValue(newPrd);
            }
            toastMessage("Updated");
            finish();
        } else {
            toastMessage("Please fill barcode and prd Name");
        }
    }

    private void getEditProduct() {

        editTextFind();

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            return;
        }
// get data via the key
        String productName = extras.getString("prdName");
        String productQuantity = extras.getString("prdQuantity");
        String productSellingPrice = extras.getString("prdSellingPrice");
        String productPurchasePrice = extras.getString("prdPurchasePrice");
        String productSupplier = extras.getString("prdSupplierName");
        String productSupplierPhone = extras.getString("prdSupplierPhone");
        String productBarcode = extras.getString("prdBarcode");
        String productImageUrl = extras.getString("prdImageUrl");
        String productGroupName = extras.getString("prdGroupName");
        String update = extras.getString("update");


        // If the intent DOES NOT contain a product content URI, then we know that we are
        // creating a new product.
        if (update.equals("update")) {
            // This is a new product, so change the app bar to say "Add a product"
            setTitle(getString(R.string.editor_activity_title_edit_product));
            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a product that hasn't been created yet.)
            invalidateOptionsMenu();
        } else {
            // Otherwise this is an existing product, so change app bar to say "Edit product"
            setTitle(getString(R.string.editor_activity_title_new_product));
            // Initialize a loader to read the product data from the database
            // and display the current values in the editor
        }

        if (productName != null) {
            prdName.setText(productName);
        } else {
            prdName.setText("");
        }
        if (productQuantity != null) {
            prdQuantity.setText(productQuantity);
        } else {
            prdQuantity.setText("");
        }
        if (productSellingPrice != null) {
            prdSellingPrice.setText(productSellingPrice);
        } else {
            prdSellingPrice.setText("");
        }
        if (productSupplier != null) {
            prdSupplier.setText(productSupplier);
        } else {
            prdSupplier.setText("");
        }
        if (productPurchasePrice != null) {
            prdPurchasingPrice.setText(productPurchasePrice);
        } else {
            prdSellingPrice.setText("");
        }
        if (productSupplierPhone != null) {
            prdSupplierPhone.setText(productSupplierPhone);
        } else {
            prdSellingPrice.setText("");
        }
        if (productBarcode != null) {
            prdBarcode.setText(productBarcode);
        } else {
            prdSellingPrice.setText("");
        }
        if (!productImageUrl.equals("")) {
            Picasso.get().load(productImageUrl).into(prdImage);
            ifPhotoIsNotChangedImageUrl = productImageUrl;
        } else {
            prdImage.setImageResource(R.drawable.noimage);
        }
        if (!productGroupName.equals("")) {
            prdGroupName.setText(productGroupName);
            beforeChangedGroupName = productGroupName;
        } else {
            prdGroupName.setText(productGroupName);
        }

    }

    private void isFormChanged() {
        editTextFind();

        TextWatcher tw = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                editTextsHaveChanged = false;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                editTextsHaveChanged = true;
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s == prdBarcode.getEditableText())
                    isBarcodeChanged = true;
                if (s == prdGroupName.getEditableText())
                    isGroupChanged = true;
            }

        };
        prdBarcode.addTextChangedListener(tw);
        prdGroupName.addTextChangedListener(tw);
        prdName.addTextChangedListener(tw);
        prdPurchasingPrice.addTextChangedListener(tw);
        prdQuantity.addTextChangedListener(tw);
        prdSellingPrice.addTextChangedListener(tw);
        prdSupplier.addTextChangedListener(tw);
        prdSupplierPhone.addTextChangedListener(tw);
    }

    private void saveProduct() {
        editTextFind();

        Product newPrd = new Product();
        newPrd.setProductName(prdName.getText().toString());
        newPrd.setProductQuantity(prdQuantity.getText().toString());
        newPrd.setSellingPrice(prdSellingPrice.getText().toString());
        newPrd.setPurchasePrice(prdPurchasingPrice.getText().toString());
        newPrd.setProductBarcode(prdBarcode.getText().toString());
        newPrd.setProductSupplier(prdSupplier.getText().toString());
        newPrd.setProductSupplierPhone(prdSupplierPhone.getText().toString());
        if (prdGroupName.getText().toString().equals(""))
            newPrd.setProductGroup(NOT_GROUPED);
        else newPrd.setProductGroup(prdGroupName.getText().toString());
        //May cause an error
        if (mImageUri != null)
            newPrd.setImage(mImageUri.toString());
        else newPrd.setImage("");
        //handle the exception if the EditText fields are null
        if (!newPrd.getProductName().equals("") && !newPrd.getProductBarcode().equals("")) {
            if (prdGroupName.getText().toString().equals("")) {
                myRef.child(userID).child("Products").child(NOT_GROUPED).child(newPrd.getProductBarcode()).setValue(newPrd);
                toastMessage("New product has been saved.");

            } else {
                myRef.child(userID).child("Products").child(newPrd.getProductGroup()).child(newPrd.getProductBarcode()).setValue(newPrd);
                myRef.child(userID).child("Group Names").child(newPrd.getProductGroup()).setValue(true);
            }
            finish();
        } else {
            toastMessage("You must fill out product name field at least to save product");
        }

    }

    @Override
    public void onBackPressed() {
        // If the product hasn't changed, continue with handling back button press
        if (editTextsHaveChanged) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Exit without saving")
                    .setMessage("Are you sure you want to exit without saving ?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        } else
            finish();
    }

    /**
     * customizable toast
     */
    private void toastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/editor_menu.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.editor_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If this is a new products, hide the "Delete" menu item.
        if (editPage) {
            MenuItem menuItem = menu.findItem(R.id.delete_button);
            menuItem.setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.save_button:
                //Save products to database
                if (!editPage) {
                    saveProduct();
                } else {
                    updateExistingProduct();
                }
                //Exit from add new products activity
                //  NavUtils.navigateUpFromSameTask(EditorActivity.this);
                // Respond to a click on the "Delete" menu option
            case R.id.delete_button:
                // Pop up confirmation dialog for deletion

                return true;

            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // Navigate back to parent activity (CatalogActivity)
                // If the product hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void scan(View view) {
        startQRScanner();

    }

    private void startQRScanner() {
        new IntentIntegrator(this).initiateScan();


    }


    private void updateText(String scanCode) {
        prdBarcode = findViewById(R.id.barCodeEditText);
        prdBarcode.setText(scanCode);
    }

    @Override
    public void handleResult(Result result) {
        String resultCode = result.getText();
        updateText(resultCode);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        prdBarcode = findViewById(R.id.barCodeEditText);
        editPageImageProgressBar = findViewById(R.id.imageProgress);

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            editPageImageProgressBar.setVisibility(View.VISIBLE);

            prdImage.setVisibility(View.INVISIBLE);

            final Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] dataBAOS = baos.toByteArray();
            final StorageReference imagesRef = mStorageRef.child(userID).child(prdBarcode.getText().toString());
            final UploadTask uploadTask = imagesRef.putBytes(dataBAOS);

            Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    return imagesRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        mImageUri = Uri.parse(downloadUri.toString());
                        toastMessage("Successfully uploaded");

                        editPageImageProgressBar.setVisibility(View.INVISIBLE);
                        prdImage.setVisibility(View.VISIBLE);
                        prdImage.setImageBitmap(bitmap);
                    } else {
                        toastMessage("Uploading failed");
                    }
                }
            });
        }
        if (requestCode == GALLERY_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri filePath = data.getData();

            //getting image from gallery
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }

            prdImage.setImageBitmap(bitmap);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] dataBAOS = baos.toByteArray();
            final StorageReference imagesRef = mStorageRef.child(userID).child(prdBarcode.getText().toString());
            final UploadTask uploadTask = imagesRef.putBytes(dataBAOS);

            final Bitmap finalBitmap = bitmap;
            Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task){
                    return imagesRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        mImageUri = Uri.parse(downloadUri.toString());
                        toastMessage("Successfully uploaded");

                        editPageImageProgressBar.setVisibility(View.INVISIBLE);
                        prdImage.setVisibility(View.VISIBLE);
                        prdImage.setImageBitmap(finalBitmap);
                    } else {
                        toastMessage("Uploading failed");
                    }
                }
            });
        }
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanningResult != null) {
            String scanContent = scanningResult.getContents();
            // String scanFormat = scanningResult.getFormatName();
            updateText(scanContent);
        } else {
            Toast toast = Toast.makeText(getApplicationContext(),
                    "No scan data received!", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

}


