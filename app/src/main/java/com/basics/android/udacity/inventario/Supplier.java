package com.basics.android.udacity.inventario;

/**
 * Representa um fornecedor.
 *
 * OBS: Para não aumentar a complexidade do aplicativo, o cadastro dos fornecedores será hardcoded
 * e manipulado pelo Contract, logo não haverá funcionalidade para manter este cadastro.
 *
 * Contêm o id, nome, telefone e email do fornecedor.
 */
public class Supplier {

    // Variável para simular o autoincremento de uma coluna de id num banco de dados.
    // Sempre que um novo objeto da classe for criado receberá um id automático
    private static int sClassId = -1;

    private int mId;
    private String mName;
    private String mPhone;
    private String mEmail;

    public Supplier(String name, String phone, String email) {
        mId = ++sClassId;
        mName = name;
        mPhone = phone;
        mEmail = email;
    }

    public int getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public String getPhone() {
        return mPhone;
    }

    public String getEmail() {
        return mEmail;
    }

    public String toString() {
        return mName;
    }
}