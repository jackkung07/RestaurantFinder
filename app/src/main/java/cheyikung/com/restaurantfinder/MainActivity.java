package cheyikung.com.restaurantfinder;

import android.app.FragmentManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import android.app.Fragment;

/**
 * Created by cheyikung on 3/19/16.
 */
public class MainActivity extends AppCompatActivity {
    private DrawerLayout mDrawer;
    private Toolbar toolbar;
    private NavigationView navigationView;
    private ActionBarDrawerToggle drawerToggle;
    private View.OnClickListener mOriginalListener;
    private boolean rotationTrigger;

    private Class fragmentClass;
    private android.app.Fragment fragmentSearch;
    private Fragment fragmentFavorite;

    private Bundle saveState;
    private boolean firstStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        saveState = savedInstanceState;
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            firstStart = true;
            rotationTrigger = false;
//            fragmentClass = FragmentFavorite.class;
//            try {
//                fragmentFavorite = (Fragment) fragmentClass.newInstance();
//            } catch (IllegalAccessException e) {
//                e.printStackTrace();
//            } catch (InstantiationException e) {
//                e.printStackTrace();
//            }
//            fragmentFavorite.setArguments(getIntent().getExtras());
//            fragmentFavorite.setRetainInstance(true);
//            getFragmentManager().beginTransaction().add(R.id.fragment_container, fragmentFavorite, "fragment_favorite").commit();
            fragmentClass = FragmentSearch.class;
            try {
                fragmentSearch = (Fragment) fragmentClass.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            fragmentSearch.setArguments(getIntent().getExtras());
            fragmentSearch.setRetainInstance(true);
            getFragmentManager().beginTransaction().add(R.id.fragment_container, fragmentSearch, "fragment_search").commit();
        } else {
            saveState = savedInstanceState;
            rotationTrigger = savedInstanceState.getBoolean("rotationTrigger");
            fragmentSearch = getFragmentManager().findFragmentByTag("fragment_search");
            fragmentFavorite = getFragmentManager().findFragmentByTag("fragment_favorite");
            Log.d("restore", "restore");

        }

        // Set a Toolbar to replace the ActionBar.
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Find our drawer view
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerClosed(View view) {
                syncActionBarArrowState();
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                // Code here will be triggered once the drawer closes as we dont want anything to happen so we leave this blank
                super.onDrawerClosed(drawerView);
                drawerToggle.setDrawerIndicatorEnabled(true);
                InputMethodManager inputMethodManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        };

        final View.OnClickListener originalToolbarListener = drawerToggle.getToolbarNavigationClickListener();
        getFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                if (getFragmentManager().getBackStackEntryCount() > 0) {
                    drawerToggle.setDrawerIndicatorEnabled(false);
                    mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                    drawerToggle.setToolbarNavigationClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            getFragmentManager().popBackStack();
                        }
                    });
                } else {
                    drawerToggle.setDrawerIndicatorEnabled(true);
                    drawerToggle.setToolbarNavigationClickListener(originalToolbarListener);
                    mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                }
            }
        });
        if (savedInstanceState != null && getFragmentManager().getBackStackEntryCount() > 0) {
            drawerToggle.setDrawerIndicatorEnabled(false);
            mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            drawerToggle.setToolbarNavigationClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getFragmentManager().popBackStack();
                }
            });
        }


        mDrawer.setDrawerListener(drawerToggle);
        navigationView = (NavigationView) findViewById(R.id.nvView);
        setupDrawerContent(navigationView);
        MenuItem menuItem = navigationView.getMenu().getItem(0);
        selectDrawerItem(menuItem);
        getFragmentManager().addOnBackStackChangedListener(mOnBackStackChangedListener);

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save custom values into the bundle
        // Always call the superclass so it can save the view hierarchy state
        rotationTrigger = true;
        outState.putBoolean("rotationTrigger", rotationTrigger);
        outState.putBoolean("firstStart", firstStart);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        firstStart = savedInstanceState.getBoolean("firstStart");
    }

    private FragmentManager.OnBackStackChangedListener
            mOnBackStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
        @Override
        public void onBackStackChanged() {
            syncActionBarArrowState();
        }
    };

    private void syncActionBarArrowState() {
        int backStackEntryCount =
                getFragmentManager().getBackStackEntryCount();
        drawerToggle.setDrawerIndicatorEnabled(backStackEntryCount == 0);

    }

    @Override
    protected void onResume() {
        super.onResume();
        // insert here your instructions
        Log.d("tag:", "onResume is called");
        drawerToggle.syncState();
    }

    @Override
    public void onBackPressed() {

        int count = getFragmentManager().getBackStackEntryCount();

        if (count == 0) {
            super.onBackPressed();
            //additional code
        } else {
            getFragmentManager().popBackStack();
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.isDrawerIndicatorEnabled() &&
                drawerToggle.onOptionsItemSelected(item)) {
            return true;
        } else if (item.getItemId() == android.R.id.home &&
                getFragmentManager().popBackStackImmediate()) {

            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        selectDrawerItem(menuItem);
                        return true;
                    }
                });
    }

    public void selectDrawerItem(MenuItem menuItem) {
        if (rotationTrigger && saveState != null) {
            menuItem.setChecked(true);
            setTitle(menuItem.getTitle());
            mDrawer.closeDrawers();
            rotationTrigger = false;
            return;
        }
        switch (menuItem.getItemId()) {
            case R.id.nav_search:
                if (firstStart) {
                    firstStart = false;
                } else {
                    Fragment fragmentTemp = getFragmentManager().findFragmentByTag("fragment_favorite");
                    if(fragmentTemp==null){
                        break;
                    }
                    getFragmentManager().beginTransaction().hide(getFragmentManager().findFragmentByTag("fragment_favorite")).show(getFragmentManager().findFragmentByTag("fragment_search")).commit();
                    getFragmentManager().beginTransaction().remove(getFragmentManager().findFragmentByTag("fragment_favorite")).commit();

                }
                break;
            case R.id.nav_favorite:
                fragmentClass = FragmentFavorite.class;
                fragmentFavorite = getFragmentManager().findFragmentByTag("fragment_favorite");
                if (fragmentFavorite != null)
                    break;
                try {
                    fragmentFavorite = (Fragment) fragmentClass.newInstance();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                }
                fragmentFavorite.setArguments(getIntent().getExtras());
                fragmentFavorite.setRetainInstance(true);

                getFragmentManager().beginTransaction().hide(getFragmentManager().findFragmentByTag("fragment_search")).add(R.id.fragment_container, fragmentFavorite, "fragment_favorite").commit();
                break;
            default:
                break;
        }
        menuItem.setChecked(true);
        setTitle(menuItem.getTitle());
        mDrawer.closeDrawers();
    }
}
