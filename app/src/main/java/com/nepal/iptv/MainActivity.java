package com.nepal.iptv;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.nepal.iptv.adapter.ChannelAdapter;
import com.nepal.iptv.model.Channel;
import com.nepal.iptv.viewmodel.ChannelViewModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class MainActivity extends AppCompatActivity implements ChannelAdapter.OnChannelClickListener {
    private static final String M3U_URL = "iptv/index.m3u";
    
    private RecyclerView recyclerView;
    private ChannelAdapter adapter;
    private LinearLayout loadingContainer;
    private ProgressBar progressBar;
    private ChannelViewModel viewModel;
    private EditText searchEditText;
    private ChipGroup categoryChipGroup;
    private ChipGroup countryChipGroup;
    private TextView channelCountText;
    
    private List<Channel> allChannels = new ArrayList<>();
    private Map<String, List<String>> categoryGroups = new HashMap<>();
    private Map<String, List<String>> countryGroups = new HashMap<>();
    private String currentSearchQuery = "";
    private String selectedCategory = "All";
    private String selectedCountry = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupRecyclerView();
        setupSearch();
        setupViewModel();
        loadChannels();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        loadingContainer = findViewById(R.id.loadingContainer);
        progressBar = findViewById(R.id.progressBar);
        searchEditText = findViewById(R.id.searchEditText);
        categoryChipGroup = findViewById(R.id.categoryChipGroup);
        countryChipGroup = findViewById(R.id.countryChipGroup);
        channelCountText = findViewById(R.id.channelCountText);
    }

    private void setupRecyclerView() {
        adapter = new ChannelAdapter(this);
        adapter.setOnFavoriteClickListener((channel, position) -> {
            channel.setFavorite(!channel.isFavorite());
            adapter.notifyItemChanged(position);
            Toast.makeText(this, 
                channel.isFavorite() ? "Added to favorites" : "Removed from favorites", 
                Toast.LENGTH_SHORT).show();
        });
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                filterChannels();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(ChannelViewModel.class);
    }

    private void loadChannels() {
        loadingContainer.setVisibility(View.VISIBLE);
        
        viewModel.getChannels(M3U_URL).observe(this, channels -> {
            loadingContainer.setVisibility(View.GONE);
            
            if (channels != null && !channels.isEmpty()) {
                allChannels = channels;
                setupFilters();
                filterChannels();
                Toast.makeText(this, "Loaded " + channels.size() + " premium channels", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to load channels", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupFilters() {
        Map<String, Set<String>> categoryMap = new HashMap<>();
        Map<String, Set<String>> countryMap = new HashMap<>();
        
        // First pass: collect all categories
        Set<String> allCategories = new TreeSet<>();
        for (Channel channel : allChannels) {
            if (channel.getGroup() != null && !channel.getGroup().isEmpty()) {
                String cleaned = cleanCategoryName(channel.getGroup());
                allCategories.add(cleaned);
            }
        }
        
        // Group categories by main keyword
        for (String category : allCategories) {
            String mainCategory = getMainCategory(category);
            
            if (!categoryMap.containsKey(mainCategory)) {
                categoryMap.put(mainCategory, new TreeSet<>());
            }
            categoryMap.get(mainCategory).add(category);
        }
        
        // Collect countries
        Set<String> allCountries = new TreeSet<>();
        for (Channel channel : allChannels) {
            if (channel.getCountry() != null && !channel.getCountry().isEmpty()) {
                String[] countryList = channel.getCountry().split(";");
                for (String country : countryList) {
                    country = country.trim();
                    if (!country.isEmpty()) {
                        allCountries.add(country);
                    }
                }
            }
        }
        
        // Group countries (just use them as-is for now)
        for (String country : allCountries) {
            countryMap.put(country, new TreeSet<>());
            countryMap.get(country).add(country);
        }

        // Convert to lists
        categoryGroups.clear();
        for (Map.Entry<String, Set<String>> entry : categoryMap.entrySet()) {
            categoryGroups.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }

        countryGroups.clear();
        for (Map.Entry<String, Set<String>> entry : countryMap.entrySet()) {
            countryGroups.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }

        // Setup category chips - only show main categories
        categoryChipGroup.removeAllViews();
        addChip(categoryChipGroup, "All", true, true);
        
        List<String> sortedCategories = new ArrayList<>(categoryGroups.keySet());
        java.util.Collections.sort(sortedCategories);
        
        // Limit to reasonable number of chips
        int count = 0;
        for (String category : sortedCategories) {
            if (count < 20) { // Max 20 category chips
                addChip(categoryChipGroup, category, false, true);
                count++;
            }
        }

        // Setup country chips - limit to popular countries
        countryChipGroup.removeAllViews();
        addChip(countryChipGroup, "All", true, false);
        
        List<String> sortedCountries = new ArrayList<>(countryGroups.keySet());
        java.util.Collections.sort(sortedCountries);
        
        // Limit to reasonable number
        count = 0;
        for (String country : sortedCountries) {
            if (count < 30) { // Max 30 country chips
                addChip(countryChipGroup, country, false, false);
                count++;
            }
        }
    }

    private String getMainCategory(String category) {
        // Extract main category word (first meaningful word)
        category = category.toLowerCase();
        
        // Common patterns to group
        if (category.contains("animation") || category.contains("anime") || category.contains("cartoon")) {
            return "Animation";
        } else if (category.contains("movie") || category.contains("film") || category.contains("cinema")) {
            return "Movies";
        } else if (category.contains("sport")) {
            return "Sports";
        } else if (category.contains("news")) {
            return "News";
        } else if (category.contains("music") || category.contains("mtv")) {
            return "Music";
        } else if (category.contains("kids") || category.contains("children")) {
            return "Kids";
        } else if (category.contains("documentary") || category.contains("docu")) {
            return "Documentary";
        } else if (category.contains("entertainment") || category.contains("variety")) {
            return "Entertainment";
        } else if (category.contains("religious") || category.contains("religion")) {
            return "Religious";
        } else if (category.contains("education") || category.contains("learning")) {
            return "Education";
        } else if (category.contains("cooking") || category.contains("food")) {
            return "Cooking";
        } else if (category.contains("travel")) {
            return "Travel";
        } else if (category.contains("lifestyle")) {
            return "Lifestyle";
        } else if (category.contains("science")) {
            return "Science";
        } else if (category.contains("auto")) {
            return "Auto";
        } else if (category.contains("business") || category.contains("finance")) {
            return "Business";
        } else if (category.contains("weather")) {
            return "Weather";
        } else if (category.contains("classic")) {
            return "Classic";
        } else if (category.contains("series") || category.contains("drama")) {
            return "Series";
        } else if (category.contains("adult") || category.contains("xxx")) {
            return "Adult";
        } else {
            // Use first word as fallback
            String[] words = category.split("\\s+");
            if (words.length > 0) {
                String firstWord = words[0];
                return firstWord.substring(0, 1).toUpperCase() + firstWord.substring(1);
            }
            return category.substring(0, 1).toUpperCase() + category.substring(1);
        }
    }

    private String getMainCountry(String country) {
        // Just return the country as-is
        return country;
    }

    private String cleanCategoryName(String category) {
        category = category.replaceAll("(?i)^(category|genre)\\s*[:-]?\\s*", "");
        category = category.trim();
        
        if (!category.isEmpty()) {
            category = category.substring(0, 1).toUpperCase() + category.substring(1);
        }
        
        return category;
    }

    private void addChip(ChipGroup chipGroup, String text, boolean checked, boolean isCategory) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCheckable(false); // Disable checkable to avoid conflicts
        chip.setChipBackgroundColorResource(checked ? R.color.luxury_gold : R.color.luxury_card_bg);
        chip.setTextColor(getColor(checked ? R.color.luxury_black : R.color.luxury_text_primary));
        chip.setChipStrokeColorResource(R.color.luxury_gold);
        chip.setChipStrokeWidth(checked ? 0 : 2);
        chip.setChipCornerRadius(60);
        chip.setTextSize(13);
        chip.setCheckedIconVisible(false);
        chip.setTag(text); // Store original text in tag
        
        chip.setOnClickListener(v -> {
            // Uncheck all other chips in the group
            for (int i = 0; i < chipGroup.getChildCount(); i++) {
                Chip otherChip = (Chip) chipGroup.getChildAt(i);
                otherChip.setChipBackgroundColorResource(R.color.luxury_card_bg);
                otherChip.setTextColor(getColor(R.color.luxury_text_primary));
                otherChip.setChipStrokeWidth(2);
            }
            
            // Check this chip
            chip.setChipBackgroundColorResource(R.color.luxury_gold);
            chip.setTextColor(getColor(R.color.luxury_black));
            chip.setChipStrokeWidth(0);
            
            if (!text.equals("All")) {
                // Show dropdown with subcategories
                List<String> subItems = isCategory ? categoryGroups.get(text) : countryGroups.get(text);
                if (subItems != null && subItems.size() > 1) {
                    showSubcategoryDialog(text, subItems, isCategory, chip);
                } else {
                    // Single item, just select it
                    if (isCategory) {
                        selectedCategory = subItems != null && !subItems.isEmpty() ? subItems.get(0) : text;
                    } else {
                        selectedCountry = subItems != null && !subItems.isEmpty() ? subItems.get(0) : text;
                    }
                    filterChannels();
                }
            } else {
                if (isCategory) {
                    selectedCategory = "All";
                } else {
                    selectedCountry = "All";
                }
                filterChannels();
            }
        });
        
        chipGroup.addView(chip);
    }

    private void showSubcategoryDialog(String mainCategory, List<String> subItems, boolean isCategory, Chip chip) {
        String[] items = subItems.toArray(new String[0]);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select " + mainCategory);
        builder.setItems(items, (dialog, which) -> {
            String selected = items[which];
            if (isCategory) {
                selectedCategory = selected;
            } else {
                selectedCountry = selected;
            }
            filterChannels();
            
            // Update chip text to show selection
            chip.setText(mainCategory + ": " + selected);
        });
        
        builder.setOnCancelListener(dialog -> {
            // If cancelled, reset to "All"
            if (isCategory) {
                selectedCategory = "All";
            } else {
                selectedCountry = "All";
            }
            filterChannels();
            
            // Reset chip selection to "All"
            ChipGroup group = isCategory ? categoryChipGroup : countryChipGroup;
            if (group.getChildCount() > 0) {
                Chip allChip = (Chip) group.getChildAt(0);
                allChip.performClick();
            }
        });
        
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(R.color.luxury_card_bg);
        dialog.show();
    }

    private void filterChannels() {
        List<Channel> filtered = new ArrayList<>();
        
        for (Channel channel : allChannels) {
            boolean matchesSearch = currentSearchQuery.isEmpty() || 
                channel.getName().toLowerCase().contains(currentSearchQuery.toLowerCase());
            
            boolean matchesCategory = selectedCategory.equals("All") ||
                (channel.getGroup() != null && cleanCategoryName(channel.getGroup()).equals(selectedCategory));
            
            boolean matchesCountry = selectedCountry.equals("All") ||
                (channel.getCountry() != null && channel.getCountry().contains(selectedCountry));
            
            if (matchesSearch && matchesCategory && matchesCountry) {
                filtered.add(channel);
            }
        }
        
        adapter.setChannels(filtered);
        channelCountText.setText(filtered.size() + " Exclusive Channels");
    }

    @Override
    public void onChannelClick(Channel channel) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("channel_name", channel.getName());
        intent.putExtra("channel_url", channel.getUrl());
        startActivity(intent);
    }
}
