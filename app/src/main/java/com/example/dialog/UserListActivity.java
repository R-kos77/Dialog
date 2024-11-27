package com.example.dialog;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.app.AlertDialog;
import android.view.ContextThemeWrapper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.Map;
import org.json.JSONObject;
import com.example.dialog.utils.LocaleHelper;

public class UserListActivity extends BaseActivity {
    private static final String TAG = "UserListActivity";
    private ListView userListView;
    private FloatingActionButton addUserFab;
    private TextView emptyTextView;
    private ArrayList<UserItem> userList;
    private ArrayAdapter<UserItem> adapter;

    private static class UserItem {
        String id;
        String name;
        String jsonData;

        UserItem(String id, String name, String jsonData) {
            this.id = id;
            this.name = name;
            this.jsonData = jsonData;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        setupToolbar();
        initializeViews();
        setupListeners();
        loadUsers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUsers();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.app_name));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.user_list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_language) {
            showLanguageDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeViews() {
        userListView = findViewById(R.id.userListView);
        addUserFab = findViewById(R.id.addUserFab);
        emptyTextView = findViewById(R.id.emptyTextView);
        
        userList = new ArrayList<>();
        adapter = new UserAdapter();
        userListView.setAdapter(adapter);
    }

    private void setupListeners() {
        addUserFab.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserProfileActivity.class);
            intent.putExtra("isNewUser", true);
            startActivity(intent);
        });
    }

    private void loadUsers() {
        userList.clear();
        SharedPreferences prefs = getSharedPreferences("UserProfiles", MODE_PRIVATE);
        Map<String, ?> allUsers = prefs.getAll();
        
        for (Map.Entry<String, ?> entry : allUsers.entrySet()) {
            try {
                String jsonData = entry.getValue().toString();
                JSONObject userJson = new JSONObject(jsonData);
                String userName = userJson.getString("firstName") + " " + userJson.getString("lastName");
                userList.add(new UserItem(entry.getKey(), userName, jsonData));
            } catch (Exception e) {
                Log.e(TAG, "Error loading user: " + e.getMessage());
            }
        }
        
        adapter.notifyDataSetChanged();
        updateEmptyView();
    }

    private void updateEmptyView() {
        if (userList.isEmpty()) {
            emptyTextView.setVisibility(View.VISIBLE);
            userListView.setVisibility(View.GONE);
        } else {
            emptyTextView.setVisibility(View.GONE);
            userListView.setVisibility(View.VISIBLE);
        }
    }

    private void loadUserAndNavigate(UserItem userItem) {
        try {
            SharedPreferences currentUserPrefs = getSharedPreferences("CurrentUser", MODE_PRIVATE);
            currentUserPrefs.edit().putString("userData", userItem.jsonData).apply();
            startActivity(new Intent(this, BloodSugarInputActivity.class));
        } catch (Exception e) {
            Log.e(TAG, "Error loading user: " + e.getMessage());
            Toast.makeText(this, "Error loading user", Toast.LENGTH_SHORT).show();
        }
    }

    private void showUserOptionsDialog(UserItem userItem) {
        try {
            if (!isFinishing()) {
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_user_options, null);
                TextView titleText = dialogView.findViewById(R.id.titleText);
                Button editButton = dialogView.findViewById(R.id.editButton);
                Button deleteButton = dialogView.findViewById(R.id.deleteButton);

                titleText.setText(userItem.name);

                AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .create();

                editButton.setOnClickListener(v -> {
                    dialog.dismiss();
                    editUserProfile(userItem);
                });

                deleteButton.setOnClickListener(v -> {
                    dialog.dismiss();
                    showDeleteUserDialog(userItem);
                });

                dialog.show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing options dialog: " + e.getMessage());
        }
    }

    private void editUserProfile(UserItem userItem) {
        try {
            Intent intent = new Intent(this, UserProfileActivity.class);
            intent.putExtra("isNewUser", false);
            intent.putExtra("userData", userItem.jsonData);
            intent.putExtra("userId", userItem.id);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error editing user: " + e.getMessage());
            Toast.makeText(this, "Error editing user", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteUser(UserItem userItem) {
        try {
            // Delete from UserProfiles
            SharedPreferences prefs = getSharedPreferences("UserProfiles", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(userItem.id);
            editor.apply();
            
            // Check and clear CurrentUser if needed
            SharedPreferences currentUserPrefs = getSharedPreferences("CurrentUser", MODE_PRIVATE);
            String currentUserJson = currentUserPrefs.getString("userData", "");
            if (!currentUserJson.isEmpty()) {
                try {
                    JSONObject currentUser = new JSONObject(currentUserJson);
                    String currentUserName = currentUser.getString("firstName") + " " + currentUser.getString("lastName");
                    if (currentUserName.equals(userItem.name)) {
                        currentUserPrefs.edit().clear().apply();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error checking current user: " + e.getMessage());
                }
            }
            
            // Remove from list and update UI
            userList.remove(userItem);
            adapter.notifyDataSetChanged();
            updateEmptyView();
            
            Toast.makeText(this, getString(R.string.user_deleted), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error deleting user: " + e.getMessage());
            Toast.makeText(this, getString(R.string.error_deleting_user), Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteUserDialog(UserItem userItem) {
        try {
            if (!isFinishing()) {
                new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.delete_user_title))
                    .setMessage(getString(R.string.delete_user_message))
                    .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                        deleteUser(userItem);
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .create()
                    .show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing delete dialog: " + e.getMessage());
            Toast.makeText(this, getString(R.string.error_deleting_user), Toast.LENGTH_SHORT).show();
        }
    }

    private class UserAdapter extends ArrayAdapter<UserItem> {
        public UserAdapter() {
            super(UserListActivity.this, R.layout.user_list_item, R.id.userName, userList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            
            UserItem userItem = getItem(position);
            TextView userName = view.findViewById(R.id.userName);
            ImageView moreOptions = view.findViewById(R.id.moreOptions);
            
            userName.setText(userItem.name);
            moreOptions.setOnClickListener(v -> showUserOptionsDialog(userItem));
            view.setOnClickListener(v -> loadUserAndNavigate(userItem));
            
            return view;
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.exit_app)
            .setMessage(R.string.exit_app_message)
            .setPositiveButton(R.string.yes, (dialog, which) -> finishAffinity())
            .setNegativeButton(R.string.no, null)
            .show();
    }
} 