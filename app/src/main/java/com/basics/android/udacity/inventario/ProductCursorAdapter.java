package com.basics.android.udacity.inventario;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.basics.android.udacity.inventario.data.InventoryContract.ProductEntry;

public class ProductCursorAdapter extends CursorAdapter {

    private Context mContext;

    public ProductCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
        mContext = context;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView nameTextView = (TextView) view.findViewById(R.id.name);
        TextView priceTextView = (TextView) view.findViewById(R.id.price);
        TextView quantityTextView = (TextView) view.findViewById(R.id.quantity);
        Button buttonSale = (Button) view.findViewById(R.id.button_sale);

        int idColumnIndex = cursor.getColumnIndex(ProductEntry._ID);
        int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
        int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
        int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);

        final int position = cursor.getInt(idColumnIndex);
        String productName = cursor.getString(nameColumnIndex);
        Double productPrice = cursor.getDouble(priceColumnIndex);
        String productQuantity = cursor.getString(quantityColumnIndex);

        buttonSale.setEnabled(false);
        final Integer quantity;
        // Se a quantidade do produto disponível for maior que zero, habilita o botão de venda do
        // produto, que realiza a venda de uma unidade atualizando a quantidade disponível
        try {
            quantity = Integer.parseInt(productQuantity);
            if (quantity > 0) {
                buttonSale.setEnabled(true);
                buttonSale.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSaleConfirmationDialog(position, quantity - 1);
                    }
                });
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        nameTextView.setText(productName);
        priceTextView.setText(context.getString(R.string.unit_product_price) + " " + Utility.formatPriceString(productPrice));
        quantityTextView.setText(context.getString(R.string.label_product_quantity) + " " + productQuantity);
    }

    // Atualiza a quantidade disponível de um produto
    private void updateQuantity(int position, int quantity) {
        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantity);
        Uri currentProductUri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, position);

        int rowsAffected = mContext.getContentResolver().update(currentProductUri, values, null, null);

        if (rowsAffected == 0) {
            Toast.makeText(mContext, mContext.getString(R.string.editor_update_product_failed),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(mContext, mContext.getString(R.string.editor_update_product_successful),
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Diálogo de confirmação para a venda
    private void showSaleConfirmationDialog(final int position, final int quantity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(R.string.sale_dialog_msg);
        builder.setPositiveButton(R.string.sale, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                updateQuantity(position, quantity);
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
}
