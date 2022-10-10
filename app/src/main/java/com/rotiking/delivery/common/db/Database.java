package com.rotiking.delivery.common.db;

import com.google.firebase.firestore.FirebaseFirestore;

public class Database {
    public static FirebaseFirestore getInstance() {
        return FirebaseFirestore.getInstance();
    }
}
