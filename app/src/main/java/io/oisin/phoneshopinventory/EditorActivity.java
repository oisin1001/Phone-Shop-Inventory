package io.oisin.phoneshopinventory;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import io.oisin.phoneshopinventory.data.InventoryContract.InventoryEntry;

public class EditorActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int EXISTING_PRODUCT_LOADER = 0;
    private Uri currentProductUri;
    private EditText productNameEditText;
    private EditText productPriceEditText;
    private EditText productQuantityEditText;
    private EditText supplierNameEditText;
    private EditText supplierNumberEditText;
    private boolean productHasChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        Intent intent = getIntent();
        currentProductUri = intent.getData();
        if (currentProductUri == null) {
            setTitle("Add a product");
            invalidateOptionsMenu();
        } else {
            setTitle("Edit a product");
            getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
        }

        productNameEditText = findViewById(R.id.edit_product_name);
        productQuantityEditText = findViewById(R.id.edit_product_quantity);
        productPriceEditText = findViewById(R.id.edit_product_price);
        supplierNameEditText = findViewById(R.id.edit_supplier_name);
        supplierNumberEditText = findViewById(R.id.edit_supplier_phone);

        productNameEditText.setOnTouchListener(touchListener);
        productQuantityEditText.setOnTouchListener(touchListener);
        productPriceEditText.setOnTouchListener(touchListener);
        supplierNameEditText.setOnTouchListener(touchListener);
        supplierNumberEditText.setOnTouchListener(touchListener);

        productPriceEditText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        Button plusButton = findViewById(R.id.plus_button);
        Button minusButton = findViewById(R.id.minus_button);
        Button orderButton = findViewById(R.id.order_button);

        minusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String quantityStr = productQuantityEditText.getText().toString().trim();
                if (!TextUtils.isEmpty(quantityStr)) {
                    int quantity = Integer.parseInt(quantityStr);
                    if (quantity > 0) {
                        productQuantityEditText.setText(Integer.toString(quantity - 1));
                    }
                }
            }
        });

        plusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String quantityStr = productQuantityEditText.getText().toString().trim();
                if (!TextUtils.isEmpty(quantityStr)) {
                    int quantity = Integer.parseInt(quantityStr);
                    productQuantityEditText.setText(Integer.toString(quantity + 1));
                }
            }
        });

        orderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String uri = "tel:" + supplierNumberEditText.getText().toString().trim();
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse(uri));
                startActivity(intent);
            }
        });
    }

    private void saveProduct() {
        String productNameStr = productNameEditText.getText().toString().trim();
        String productQuantityStr = productQuantityEditText.getText().toString().trim();
        String productPriceStr = productPriceEditText.getText().toString().trim();
        String supplierNameStr = supplierNameEditText.getText().toString().trim();
        String supplierNumberStr = supplierNumberEditText.getText().toString().trim();

        if (TextUtils.isEmpty(productNameStr) || TextUtils.isEmpty(productQuantityStr) ||
                TextUtils.isEmpty(productPriceStr) || TextUtils.isEmpty(supplierNameStr)
                || TextUtils.isEmpty(supplierNumberStr)) {
            Toast.makeText(this, "No field can be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        int productQuantity = Integer.parseInt(productQuantityStr);
        double productPrice = Double.parseDouble(productPriceStr);

        if (productQuantity < 0) {
            Toast.makeText(this, "Quantity can't be negative", Toast.LENGTH_SHORT).show();
            return;
        }

        if (productPrice < 0.0) {
            Toast.makeText(this, "Price can't be negative", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.put(InventoryEntry.COLUMN_PRODUCT_NAME, productNameStr);
        values.put(InventoryEntry.COLUMN_PRODUCT_QUANTITY, productQuantity);
        values.put(InventoryEntry.COLUMN_PRODUCT_PRICE, productPrice);
        values.put(InventoryEntry.COLUMN_SUPPLIER_NAME, supplierNameStr);
        values.put(InventoryEntry.COLUMN_SUPPLIER_PHONE, supplierNumberStr);

        Uri newUri;

        if (getIntent().getData() == null) {
            newUri = getContentResolver().insert(InventoryEntry.CONTENT_URI, values);

            if (newUri == null) {
                Toast.makeText(this, "Failed to add product",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Product added!",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            int rowsAffected = getContentResolver().update(currentProductUri, values, null, null);

            if (rowsAffected == 0) {
                Toast.makeText(this, "Failed to update product",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Product updated!",
                        Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                saveProduct();
                return true;
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            case android.R.id.home:
                if (!productHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] projection = {
                InventoryEntry._ID,
                InventoryEntry.COLUMN_PRODUCT_NAME,
                InventoryEntry.COLUMN_PRODUCT_PRICE,
                InventoryEntry.COLUMN_PRODUCT_QUANTITY,
                InventoryEntry.COLUMN_SUPPLIER_NAME,
                InventoryEntry.COLUMN_SUPPLIER_PHONE };

        return new CursorLoader(this,
                currentProductUri,
                projection,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        if (cursor.moveToFirst()) {
            int productNameIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_NAME);
            int productPriceIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_PRICE);
            int productQuantityIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_QUANTITY);
            int supplierNameIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_SUPPLIER_NAME);
            int supplierPhoneIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_SUPPLIER_PHONE);

            String name = cursor.getString(productNameIndex);
            double price = cursor.getDouble(productPriceIndex);
            int quantity = cursor.getInt(productQuantityIndex);
            String supplierName = cursor.getString(supplierNameIndex);
            String phone = cursor.getString(supplierPhoneIndex);

            productNameEditText.setText(name);
            supplierNameEditText.setText(supplierName);
            supplierNumberEditText.setText(phone);
            String priceStr = Double.toString(price);
            productPriceEditText.setText(priceStr);
            String quantityStr = Integer.toString(quantity);
            productQuantityEditText.setText(quantityStr);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        productNameEditText.setText("");
        supplierNameEditText.setText("");
        supplierNumberEditText.setText("");
        productQuantityEditText.setText("");
        productPriceEditText.setText("");
    }

    private View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            productHasChanged = true;
            return false;
        }
    };

    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton("Discard", discardButtonClickListener);
        builder.setNegativeButton("Continue", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        if (!productHasChanged) {
            super.onBackPressed();
            return;
        }

        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                };

        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (currentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                deleteProduct();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void deleteProduct() {
        if (getIntent().getData() != null) {
            int rowsDeleted = getContentResolver().delete(getIntent().getData(), null, null);

            if (rowsDeleted == 0) {
                Toast.makeText(this, "Failed to delete product", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Product successfully deleted", Toast.LENGTH_SHORT).show();
            }
        }

        finish();
    }
}