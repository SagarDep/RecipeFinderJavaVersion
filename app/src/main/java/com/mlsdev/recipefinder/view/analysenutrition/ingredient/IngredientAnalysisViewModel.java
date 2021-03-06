package com.mlsdev.recipefinder.view.analysenutrition.ingredient;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.mlsdev.recipefinder.R;
import com.mlsdev.recipefinder.data.entity.nutrition.NutritionAnalysisResult;
import com.mlsdev.recipefinder.data.entity.nutrition.TotalNutrients;
import com.mlsdev.recipefinder.data.source.remote.ParameterKeys;
import com.mlsdev.recipefinder.data.source.repository.DataRepository;
import com.mlsdev.recipefinder.view.OnKeyboardStateChangedListener;
import com.mlsdev.recipefinder.view.listener.OnIngredientAnalyzedListener;
import com.mlsdev.recipefinder.view.utils.DiagramUtils;
import com.mlsdev.recipefinder.view.viewmodel.BaseViewModel;

import java.util.ArrayList;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class IngredientAnalysisViewModel extends BaseViewModel implements LifecycleObserver {
    private OnIngredientAnalyzedListener onIngredientAnalyzedListener;
    private OnKeyboardStateChangedListener keyboardListener;
    public final ObservableField<String> ingredientText = new ObservableField<>("");
    public final ObservableField<String> nutrientText = new ObservableField<>("");
    public final ObservableField<String> fatText = new ObservableField<>("");
    public final ObservableField<String> proteinText = new ObservableField<>("");
    public final ObservableField<String> carbsText = new ObservableField<>("");
    public final ObservableField<String> energyText = new ObservableField<>("");
    public final ObservableInt diagramVisibility = new ObservableInt(View.GONE);
    public final ObservableInt energyLabelVisibility = new ObservableInt(View.GONE);
    public final ObservableInt fatLabelVisibility = new ObservableInt(View.GONE);
    public final ObservableInt carbsLabelVisibility = new ObservableInt(View.GONE);
    public final ObservableInt proteinLabelVisibility = new ObservableInt(View.GONE);
    public final ObservableInt analysisResultsWrapperVisibility = new ObservableInt(View.INVISIBLE);
    public final ObservableBoolean ingredientTextFocused = new ObservableBoolean(false);
    private TotalNutrients totalNutrients;
    private DiagramUtils diagramUtils;

    @Inject
    public IngredientAnalysisViewModel(DiagramUtils diagramUtils, DataRepository repository) {
        this.diagramUtils = diagramUtils;
        this.repository = repository;
    }

    public void setKeyboardListener(OnKeyboardStateChangedListener keyboardListener) {
        this.keyboardListener = keyboardListener;
    }

    public void setOnIngredientAnalyzedListener(OnIngredientAnalyzedListener onIngredientAnalyzedListener) {
        this.onIngredientAnalyzedListener = onIngredientAnalyzedListener;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    void start() {
        prepareDiagramData(totalNutrients);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    void stop() {
        Log.d("RF", "lifecycle stop");
    }

    public void onAnalyzeButtonClick(View view) {
        keyboardListener.hideKeyboard();

        if (ingredientText.get().isEmpty()) {
            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ingredientTextFocused.set(true);
                    keyboardListener.showKeyboard();
                }
            };

            actionListener.showSnackbar(R.string.error_empty_ingredient_field, R.string.btn_fill_in, listener);
            return;
        }

        actionListener.showProgressDialog(true, "Analysing...");
        subscriptions.clear();

        Map<String, String> params = new ArrayMap<>();
        params.put(ParameterKeys.INGREDIENT, ingredientText.get());
        repository.getIngredientData(params)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new SingleObserver<NutritionAnalysisResult>() {

                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        subscriptions.add(d);
                    }

                    @Override
                    public void onSuccess(@io.reactivex.annotations.NonNull NutritionAnalysisResult result) {
                        actionListener.showProgressDialog(false, null);
                        TotalNutrients totalNutrients = result.getTotalNutrients();
                        analysisResultsWrapperVisibility.set(View.VISIBLE);

                        nutrientText.set(ingredientText.get());
                        fatText.set(totalNutrients.getFat() != null
                                ? result.getTotalNutrients().getFat().getFormattedFullText() : "");

                        proteinText.set(totalNutrients.getProtein() != null
                                ? result.getTotalNutrients().getProtein().getFormattedFullText() : "");

                        carbsText.set(totalNutrients.getCarbs() != null
                                ? result.getTotalNutrients().getCarbs().getFormattedFullText() : "");

                        energyText.set(totalNutrients.getEnergy() != null
                                ? result.getTotalNutrients().getEnergy().getFormattedFullText() : "");

                        carbsLabelVisibility.set(carbsText.get().isEmpty() ? View.GONE : View.VISIBLE);
                        proteinLabelVisibility.set(proteinText.get().isEmpty() ? View.GONE : View.VISIBLE);
                        fatLabelVisibility.set(fatText.get().isEmpty() ? View.GONE : View.VISIBLE);
                        energyLabelVisibility.set(energyText.get().isEmpty() ? View.GONE : View.VISIBLE);

                        IngredientAnalysisViewModel.this.totalNutrients = totalNutrients;
                        prepareDiagramData(totalNutrients);
                    }

                    @Override
                    public void onError(Throwable e) {
                        showError(e);
                    }

                });

    }

    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {

        if (actionId == KeyEvent.ACTION_DOWN || actionId == EditorInfo.IME_ACTION_DONE) {
            onAnalyzeButtonClick(null);
            keyboardListener.hideKeyboard();
            return true;
        }

        return false;
    }

    private void prepareDiagramData(TotalNutrients nutrients) {
        if (nutrients == null)
            return;

        ArrayList<PieEntry> entries = diagramUtils.preparePieEntries(nutrients);
        diagramVisibility.set(entries.isEmpty() ? View.GONE : View.VISIBLE);
        PieDataSet pieDataSet = diagramUtils.createPieDataSet(entries, "Nutrients", null);
        PieData pieData = diagramUtils.createPieData(pieDataSet);

        onIngredientAnalyzedListener.onIngredientAnalyzed(pieData);
        diagramVisibility.set(View.VISIBLE);
    }
}
