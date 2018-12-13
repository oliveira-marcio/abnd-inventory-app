package com.basics.android.udacity.inventario;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.basics.android.udacity.inventario.data.InventoryContract.ProductEntry;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.basics.android.udacity.inventario.R.string.editor_permissions_message;
import static com.basics.android.udacity.inventario.data.InventoryContract.ProductEntry.SUPPLIERS;

public class EditorActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    // Elementos de interface
    private RelativeLayout mImageHolder;
    private ImageView mProductImageView;
    private ImageView mIconImageCapture;
    private EditText mEditProductName;
    private EditText mEditProductPrice;
    private TextView mLabelProductQuantity;
    private Button mButtonSale;
    private EditText mSaleTotal;
    private Button mButtonShipment;
    private EditText mShipmentTotal;
    private Spinner mSupplierSpinner;
    private TextView mSupplierTextView;
    private Button mButtonOrder;

    private static final int EXISTING_PRODUCT_LOADER = 0;

    // URI de conteúdo para o produto existente (nulo se for um novo produto)
    private Uri mCurrentProductUri;

    // Variáveis de indicadores do editor. Respectivamente: se está no modo de edição/visualização,
    // se algum campo obrigatório não foi preenchido e se algum campo foi alterado.
    private boolean mScreenInEditMode = false;
    private boolean mInformationMissing = true;
    private boolean mProductHasChanged = false;
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (mScreenInEditMode) {
                mProductHasChanged = true;
            }
            return false;
        }
    };

    // Variáveis para uso de imagens. Respectivamente: o bitmap convertido e usado na ImageView,
    // a origem da imagem (câmera ou galeria) e caminho do arquivo gerado pela câmera.
    private Bitmap mProductBitmapImage;
    private String mImageSource;
    private String mCurrentPhotoPath;

    // Quantidade e fornecedor do produto
    private int mProductQuantity = 0;
    private Supplier mSupplier;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        initializeUiElements();

        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        if (mCurrentProductUri == null) {
            mScreenInEditMode = true;
        } else {
            mScreenInEditMode = false;
            getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
        }

        changeEditorMode(mScreenInEditMode);

        setupSpinner();
    }

    private void initializeUiElements() {
        mImageHolder = (RelativeLayout) findViewById(R.id.image_holder);
        mProductImageView = (ImageView) findViewById(R.id.product_image);
        mIconImageCapture = (ImageView) findViewById(R.id.ic_image_capture);
        mEditProductName = (EditText) findViewById(R.id.edit_product_name);
        mEditProductPrice = (EditText) findViewById(R.id.edit_product_price);
        mLabelProductQuantity = (TextView) findViewById(R.id.label_product_quantity);
        mButtonSale = (Button) findViewById(R.id.button_sale);
        mSaleTotal = (EditText) findViewById(R.id.edit_sale_total);
        mButtonShipment = (Button) findViewById(R.id.button_shipment);
        mShipmentTotal = (EditText) findViewById(R.id.edit_shipment_total);
        mSupplierSpinner = (Spinner) findViewById(R.id.spinner_supplier);
        mSupplierTextView = (TextView) findViewById(R.id.text_supplier);
        mButtonOrder = (Button) findViewById(R.id.button_order);

        mButtonSale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decreaseQuantity();
            }
        });

        mButtonShipment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                increaseQuantity();
            }
        });

        mImageHolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImageSource();
            }
        });

        mButtonOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectSupplierContact();
            }
        });
    }

    // Aumenta a quantidade do produto de acordo com o valor de recebimento preenchido
    private void increaseQuantity() {
        String quantityString = mShipmentTotal.getText().toString().trim();
        if (TextUtils.isEmpty(quantityString)) {
            Toast.makeText(this, R.string.editor_shipment_quantity_error, Toast.LENGTH_SHORT).show();
            return;
        }

        int quantity = Integer.parseInt(quantityString);
        if (quantity < 1) {
            Toast.makeText(this, R.string.editor_shipment_quantity_error, Toast.LENGTH_SHORT).show();
            return;
        }

        mProductQuantity += quantity;

        mLabelProductQuantity.setText(getString(R.string.label_product_quantity) + " " + mProductQuantity);
        mSaleTotal.setText("");
        mShipmentTotal.setText("");
    }

    // Diminui a quantidade do produto (até zero) de acordo com o valor de venda preenchido.
    private void decreaseQuantity() {
        String quantityString = mSaleTotal.getText().toString().trim();
        if (TextUtils.isEmpty(quantityString)) {
            Toast.makeText(this, R.string.editor_sale_quantity_error, Toast.LENGTH_SHORT).show();
            return;
        }

        int quantity = Integer.parseInt(quantityString);
        if (quantity < 1) {
            Toast.makeText(this, R.string.editor_sale_quantity_error, Toast.LENGTH_SHORT).show();
            return;
        }

        mProductQuantity -= quantity;
        if (mProductQuantity < 0) {
            mProductQuantity = 0;
        }

        mSaleTotal.setText("");
        mShipmentTotal.setText("");
        mLabelProductQuantity.setText(getString(R.string.label_product_quantity) + " " + mProductQuantity);
    }

    /**
     *     Altera o editor para os 3 modos possíveis, setando os campos correspondentes e menus:
     *     - Novo Produto (apenas nome, preço, fornecedor e imagem podem ser preenchidos)
     *     - Edição do Produto (igual ao anterior, além da opção de manipular a quantidade)
     *     - Visualização do Produto (apenas consulta os dados, com opção de contactar o fornecedor)
     */
    private void changeEditorMode(boolean mode) {
        if (mode) {
            // Editor em modo de edição
            mImageHolder.setEnabled(true);
            mIconImageCapture.setVisibility(View.VISIBLE);
            mEditProductName.setEnabled(true);
            mEditProductName.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
            mEditProductPrice.setEnabled(true);
            mEditProductPrice.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            mSupplierSpinner.setVisibility(View.VISIBLE);
            mSupplierTextView.setVisibility(View.GONE);
            mButtonOrder.setVisibility(View.GONE);

            if (mCurrentProductUri == null) {
                // Caso seja um novo produto
                setTitle(getString(R.string.editor_activity_title_new_product));
                mLabelProductQuantity.setVisibility(View.GONE);
                mButtonSale.setVisibility(View.GONE);
                mSaleTotal.setVisibility(View.GONE);
                mButtonShipment.setVisibility(View.GONE);
                mShipmentTotal.setVisibility(View.GONE);

                mButtonSale.setOnTouchListener(null);
                mSaleTotal.setOnTouchListener(null);
                mButtonShipment.setOnTouchListener(null);
                mShipmentTotal.setOnTouchListener(null);
            } else {
                // Ou produto existente
                setTitle(getString(R.string.editor_activity_title_edit_product));
                mLabelProductQuantity.setVisibility(View.VISIBLE);
                mButtonSale.setVisibility(View.VISIBLE);
                mSaleTotal.setVisibility(View.VISIBLE);
                mButtonShipment.setVisibility(View.VISIBLE);
                mShipmentTotal.setVisibility(View.VISIBLE);

                mEditProductPrice.setText(mEditProductPrice.getText().toString().replace(",", "."));
                mEditProductPrice.clearFocus();

                mButtonSale.setOnTouchListener(mTouchListener);
                mSaleTotal.setOnTouchListener(mTouchListener);
                mButtonShipment.setOnTouchListener(mTouchListener);
                mShipmentTotal.setOnTouchListener(mTouchListener);
            }

            mImageHolder.setOnTouchListener(mTouchListener);
            mEditProductName.setOnTouchListener(mTouchListener);
            mEditProductPrice.setOnTouchListener(mTouchListener);
            mSupplierSpinner.setOnTouchListener(mTouchListener);
        } else {
            // Editor em modo de visualização
            mImageHolder.setEnabled(false);
            mIconImageCapture.setVisibility(View.GONE);
            mEditProductName.setEnabled(false);
            mEditProductName.setInputType(InputType.TYPE_NULL);
            mEditProductPrice.setEnabled(false);
            mEditProductPrice.setInputType(InputType.TYPE_NULL);
            mLabelProductQuantity.setVisibility(View.VISIBLE);
            mButtonSale.setVisibility(View.GONE);
            mSaleTotal.setVisibility(View.GONE);
            mButtonShipment.setVisibility(View.GONE);
            mShipmentTotal.setVisibility(View.GONE);
            mSupplierSpinner.setVisibility(View.GONE);
            mSupplierTextView.setVisibility(View.VISIBLE);
            mButtonOrder.setVisibility(View.VISIBLE);

            mImageHolder.setOnTouchListener(null);
            mEditProductName.setOnTouchListener(null);
            mEditProductPrice.setOnTouchListener(null);
            mSupplierSpinner.setOnTouchListener(null);
            mButtonSale.setOnTouchListener(null);
            mSaleTotal.setOnTouchListener(null);
            mButtonShipment.setOnTouchListener(null);
            mShipmentTotal.setOnTouchListener(null);

            setTitle(getString(R.string.editor_activity_title_view_product));
        }

        invalidateOptionsMenu();
    }

    // Exibe diálogo para selecionar a forma de contato com o fornecedor (email ou telefone)
    private void selectSupplierContact() {
        final String[] items = getResources().getStringArray(R.array.supplier_contact_options);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.supplier_contact_label);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                boolean result = Utility.checkPermission(EditorActivity.this);

                if (items[item].equals(getString(R.string.supplier_contact_email))) {
                    if (result) {
                        orderSupplierByEmail();
                    }

                } else if (items[item].equals(getString(R.string.supplier_contact_phone))) {
                    if (result) {
                        orderSupplierByPhone();
                    }

                } else if (items[item].equals(getString(R.string.cancel))) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private void orderSupplierByEmail() {
        String orderMessage = getString(R.string.email_text_introdution) + " " +
                mSupplier.getName() + ",\n\n" +
                getString(R.string.email_text_body1) + " \"" +
                mEditProductName.getText().toString() + "\" " +
                getString(R.string.email_text_body2) + "\n\n" +
                getString(R.string.email_text_end) + "\n\n" +
                getString(R.string.email_text_signature);

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{mSupplier.getEmail()});
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject));
        intent.putExtra(Intent.EXTRA_TEXT, orderMessage);

        // Métodos extraídos do StackOverFlow para anexar a imagem do produto no e-mail de contato
        File tempFile = null;
        try {
            tempFile = Utility.createImageFile(getCacheDir());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (tempFile != null) {
            if (Utility.saveBitmapToFile(tempFile, mProductBitmapImage)) {
                Uri uri = FileProvider.getUriForFile(
                        this, Utility.FILE_PROVIDER_AUTHORITY, tempFile);
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            }
        }

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private void orderSupplierByPhone() {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + mSupplier.getPhone()));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private void setupSpinner() {

        ArrayAdapter supplierSpinnerAdapter = new ArrayAdapter(this,
                android.R.layout.simple_spinner_item, SUPPLIERS);

        supplierSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        mSupplierSpinner.setAdapter(supplierSpinnerAdapter);

        mSupplierSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSupplier = (Supplier) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mSupplier = null;
            }
        });
    }

    // Exibe diálogo para selecionar a origem da imagem (câmera ou galeria)
    private void selectImageSource() {
        final String[] items = getResources().getStringArray(R.array.image_source_options);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.image_source_label);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                boolean result = Utility.checkPermission(EditorActivity.this);

                if (items[item].equals(getString(R.string.image_source_camera))) {
                    mImageSource = getString(R.string.image_source_camera);
                    if (result) {
                        dispatchCameraIntent();
                    }

                } else if (items[item].equals(getString(R.string.image_source_gallery))) {
                    mImageSource = getString(R.string.image_source_gallery);
                    if (result) {
                        dispatchGalleryIntent();
                    }

                } else if (items[item].equals(getString(R.string.cancel))) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private void dispatchGalleryIntent() {
        Intent intent;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }

        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, getString(R.string.image_gallery_label)), Utility.REQUEST_FILE);
    }

    private void dispatchCameraIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File photoFile = null;
        mCurrentPhotoPath = null;

        try {
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            photoFile = Utility.createImageFile(storageDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (photoFile != null) {
            mCurrentPhotoPath = photoFile.getAbsolutePath();
            Uri photoURI = FileProvider.getUriForFile(this, Utility.FILE_PROVIDER_AUTHORITY, photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(intent, Utility.REQUEST_CAMERA);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case Utility.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (mImageSource.equals(getString(R.string.image_source_camera))) {
                        dispatchCameraIntent();
                    } else if (mImageSource.equals(getString(R.string.image_source_gallery))) {
                        dispatchGalleryIntent();
                    }
                } else {
                    Toast.makeText(this, editor_permissions_message, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case Utility.REQUEST_FILE:
                    mProductBitmapImage = Utility.getImageFromGallery(this, getContentResolver(), data);
                    break;

                case Utility.REQUEST_CAMERA:
                    mProductBitmapImage = Utility.getImageFromCamera(this, mCurrentPhotoPath);
                    break;
            }

            if (mProductBitmapImage != null) {
                mProductImageView.setImageBitmap(mProductBitmapImage);
            } else {
                mProductImageView.setImageResource(R.drawable.img_placeholder);
            }
        }
    }

    private void saveProduct() {
        String nameString = mEditProductName.getText().toString().trim();
        String priceString = mEditProductPrice.getText().toString().trim().replace(",", ".");
        int supplierId = mSupplier.getId();

        // Em caso de nenhum campo preenchido, para um novo produto, nenhuma ação é tomada, pode
        // ter sido um clique acidental do usuário no botão de salvar
        if (mCurrentProductUri == null && mProductBitmapImage == null &&
                TextUtils.isEmpty(nameString) &&
                TextUtils.isEmpty(priceString) && supplierId == 0) {
            return;
        }

        // Checa o preenchimento dos campos obrigatórios
        if (mProductBitmapImage == null || TextUtils.isEmpty(nameString) || supplierId == 0) {
            Toast.makeText(this, R.string.editor_information_missing, Toast.LENGTH_SHORT).show();
            mInformationMissing = true;
            return;
        }

        mInformationMissing = false;

        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_NAME, nameString);
        values.put(ProductEntry.COLUMN_PRODUCT_SUPPLIER, supplierId);
        values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, mProductQuantity);
        values.put(ProductEntry.COLUMN_PRODUCT_IMAGE, Utility.getByteArrayfromBitmap(mProductBitmapImage));


        double price = 0;
        if (!TextUtils.isEmpty(priceString)) {
            price = Double.parseDouble(priceString);
        }
        values.put(ProductEntry.COLUMN_PRODUCT_PRICE, price);

        if (mCurrentProductUri == null) {
            createNewProduct(values);
        } else {
            updateCurrentProduct(values);
        }
    }

    private void createNewProduct(ContentValues values){
        Uri newUri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);
        if (newUri == null) {
            Toast.makeText(this, getString(R.string.editor_insert_product_failed),
                    Toast.LENGTH_SHORT).show();
        } else {
            mCurrentProductUri = newUri;
            getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
            Toast.makeText(this, getString(R.string.editor_insert_product_successful),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void updateCurrentProduct(ContentValues values){
        int rowsAffected = getContentResolver().update(mCurrentProductUri, values, null, null);

        if (rowsAffected == 0) {
            Toast.makeText(this, getString(R.string.editor_update_product_failed),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getString(R.string.editor_update_product_successful),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    /**
     * Exibe os ítens de menu de acordo com o modo de edição. O botão "delete" só aparece se houver
     * a URI presente do produto
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem editMenuItem = menu.findItem(R.id.action_edit);
        editMenuItem.setVisible(!mScreenInEditMode);

        MenuItem saveMenuItem = menu.findItem(R.id.action_save);
        saveMenuItem.setVisible(mScreenInEditMode);

        MenuItem deleteMenuItem = menu.findItem(R.id.action_delete);
        deleteMenuItem.setVisible((mScreenInEditMode && mCurrentProductUri != null) ? true : false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                saveProduct();
                if (mCurrentProductUri != null && !mInformationMissing) {
                    mScreenInEditMode = false;
                    mProductHasChanged = false;
                    changeEditorMode(mScreenInEditMode);
                }
                return true;
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            case R.id.action_edit:
                mScreenInEditMode = true;
                changeEditorMode(mScreenInEditMode);
                return true;
            /**
             * Aqui é necessário tratar a opção de voltar de acordo com os 3 seguintes possíveis cenários:
             * - Edição de um novo produto: se houve alterações, mostra uma confirmação para descartar
             * os dados e retorna para a tela principal
             * - Edição de um produto existente: se houve alterações, mostra uma confirmação para
             * descartar os dados e retorna para a visualização do produto
             * - Visualização de um produto: apenas retorna para a tela principal
             */
            case android.R.id.home:
                if (!mProductHasChanged) {
                    if (mScreenInEditMode && mCurrentProductUri != null) {
                        mScreenInEditMode = false;
                        changeEditorMode(mScreenInEditMode);
                    } else {
                        NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    }
                    return true;
                }

                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (mScreenInEditMode && mCurrentProductUri != null) {
                                    mProductHasChanged = false;
                                    mScreenInEditMode = false;
                                    changeEditorMode(mScreenInEditMode);
                                    getLoaderManager().restartLoader(EXISTING_PRODUCT_LOADER, null, EditorActivity.this);
                                } else {
                                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                                }
                            }
                        };

                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Aqui é necessário tratar a opção de voltar de acordo com os 3 seguintes possíveis cenários:
     * - Edição de um novo produto: se houve alterações, mostra uma confirmação para descartar
     * os dados e retorna para a tela principal
     * - Edição de um produto existente: se houve alterações, mostra uma confirmação para
     * descartar os dados e retorna para a visualização do produto
     * - Visualização de um produto: apenas retorna para a tela principal
     */
    @Override
    public void onBackPressed() {
        if (!mProductHasChanged) {
            if (mScreenInEditMode && mCurrentProductUri != null) {
                mScreenInEditMode = false;
                changeEditorMode(mScreenInEditMode);
            } else {
                super.onBackPressed();
            }
            return;
        }

        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (mScreenInEditMode && mCurrentProductUri != null) {
                            mProductHasChanged = false;
                            mScreenInEditMode = false;
                            changeEditorMode(mScreenInEditMode);
                            getLoaderManager().restartLoader(EXISTING_PRODUCT_LOADER, null, EditorActivity.this);
                        } else {
                            finish();
                        }
                    }
                };

        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this, mCurrentProductUri, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        if (cursor.moveToFirst()) {
            int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
            int imageColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_IMAGE);
            int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);
            int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
            int supplierColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_SUPPLIER);

            String name = cursor.getString(nameColumnIndex);
            byte[] image = cursor.getBlob(imageColumnIndex);
            mProductQuantity = cursor.getInt(quantityColumnIndex);
            double price = cursor.getDouble(priceColumnIndex);
            Integer supplierId = cursor.getInt(supplierColumnIndex);

            mEditProductName.setText(name);
            mLabelProductQuantity.setText(getString(R.string.label_product_quantity) + " " + mProductQuantity);
            mEditProductPrice.setText(Utility.formatPriceString(price));

            if (image != null) {
                mProductBitmapImage = Utility.getBitmapFromByteArray(image);
                mProductImageView.setImageBitmap(mProductBitmapImage);
            } else {
                mProductImageView.setImageResource(R.drawable.img_placeholder);
            }

            if (supplierId == null || !ProductEntry.isValidSupplier(supplierId)) {
                mSupplierSpinner.setSelection(0);
            } else {
                mSupplierSpinner.setSelection(supplierId);
                mSupplier = SUPPLIERS[supplierId];
                mSupplierTextView.setText(mSupplier.getName());
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mEditProductName.setText("");
        mEditProductPrice.setText("");
        mProductImageView.setImageResource(R.drawable.img_placeholder);
        mSupplierSpinner.setSelection(0);
        mSupplierTextView.setText("");
    }


    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                deletePet();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void deletePet() {
        if (mCurrentProductUri != null) {
            int rowsDeleted = getContentResolver().delete(mCurrentProductUri, null, null);
            if (rowsDeleted == 0) {
                Toast.makeText(this, getString(R.string.editor_delete_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.editor_delete_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }

        finish();
    }
}