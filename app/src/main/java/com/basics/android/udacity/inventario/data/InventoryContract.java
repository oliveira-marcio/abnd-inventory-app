package com.basics.android.udacity.inventario.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

import com.basics.android.udacity.inventario.Supplier;

public final class InventoryContract {

    private InventoryContract() {
    }

    public static final String CONTENT_AUTHORITY = "com.basics.android.udacity.inventario";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_PRODUCTS = "products";

    public static final class ProductEntry implements BaseColumns {

        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_PRODUCTS);

        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCTS;

        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCTS;

        public final static String TABLE_NAME = "products";

        public final static String _ID = BaseColumns._ID;
        public final static String COLUMN_PRODUCT_NAME = "name";
        public final static String COLUMN_PRODUCT_IMAGE = "image";
        public final static String COLUMN_PRODUCT_PRICE = "price";
        public final static String COLUMN_PRODUCT_QUANTITY = "quantity";
        public final static String COLUMN_PRODUCT_SUPPLIER = "supplier";

        /**
         * Representa o cadastro hardcoded dos fornecedores.
         *
         * OBS: o id = 0 é apenas um fornecedor falso para compor o primeiro item do Spinner
         * no editor, que será tratado na validação abaixo
         */
        public final static Supplier[] SUPPLIERS = new Supplier[]{
                new Supplier("(Selecione)", null, null),
                new Supplier("ABCD SUPPLIERS", "3333-4444", "abcd@gmail.com"),
                new Supplier("XPTO Peças e Equipamentos", "2222-4444", "xpto@gmail.com"),
                new Supplier("Rei do Material", "1111-4444", "rei_dos_materiais@gmail.com"),
                new Supplier("Tech Store", "5555-4444", "techstore@gmail.com")
        };

        // Testa se o id fornecido pertence a algum fornecedor
        public static boolean isValidSupplier(int id) {
            if (id > 0) {
                for (int i = 0; i < SUPPLIERS.length; i++) {
                    if (SUPPLIERS[i].getId() == id) {
                        return true;
                    }
                }
            }

            return false;
        }
    }
}

