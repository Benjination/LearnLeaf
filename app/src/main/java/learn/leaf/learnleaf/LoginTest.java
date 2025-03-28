package learn.leaf.learnleaf;

/*
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class LoginTest {

    @Rule
    public ActivityTestRule<Login> activityRule = new ActivityTestRule<>(Login.class);

    @Mock
    private FirebaseAuth mockAuth;

    @Mock
    private FirebaseUser mockUser;

    private Login loginActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ActivityScenario<Login> scenario = ActivityScenario.launch(Login.class);
        scenario.onActivity(activity -> loginActivity = activity);
    }

    @Test
    public void testIsValidEmail() {
        assertTrue(loginActivity.isValidEmail("test@example.com"));
        assertFalse(loginActivity.isValidEmail("invalid-email"));
        assertFalse(loginActivity.isValidEmail(""));
    }

    @Test
    public void testSignInWithEmail_Success() {
        when(mockAuth.getCurrentUser()).thenReturn(mockUser);

        loginActivity.runOnUiThread(() -> {
            loginActivity.email.setText("test@example.com");
            loginActivity.password.setText("password123");
            loginActivity.signInWithEmail();
        });
    }

    @Test
    public void testForgotPassword_InvalidEmail() {
        loginActivity.runOnUiThread(() -> {
            loginActivity.email.setText("invalid-email");
            loginActivity.forgotPassword();

            Toast toast = Toast.makeText(loginActivity, "Please enter a valid email", Toast.LENGTH_SHORT);
            assertEquals("Please enter a valid email", toast.getView().toString());
        });
    }
}

*/