package com.mlsdev.recipefinder;

import android.support.annotation.IdRes;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;

import com.mlsdev.recipefinder.view.MainActivity;

import org.junit.Rule;
import org.junit.Test;

public class MainActivityTest {
    @Rule
    public ActivityTestRule<MainActivity> testRule = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void testSelectNavigationTab() {
        // Select the Analyse Nutrition tab
        performClickUponTab(R.id.action_analyse_nutrition);
        assertTabOpened(R.id.ll_analyse_nutrition);

        // Select the Favorite Recipes tab
        performClickUponTab(R.id.action_favorites);
        assertTabOpened(R.id.rl_favorite_recipes);

        // Select the Search Recipes tab
        performClickUponTab(R.id.action_search_recipe);
        assertTabOpened(R.id.cl_search_recipes);
    }

    private void performClickUponTab(@IdRes int tabId) {
        Espresso.onView(ViewMatchers.withId(tabId)).perform(ViewActions.click());
    }

    private void assertTabOpened(@IdRes int layoutId) {
        Espresso.onView(ViewMatchers.withId(layoutId))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }
}
