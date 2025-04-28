package com.bh.beanie.user.fragment

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bh.beanie.R
import com.bh.beanie.databinding.FragmentLanguageSettingsBinding
import java.util.Locale

class LanguageSettingsFragment : Fragment() {
    private var _binding: FragmentLanguageSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = requireActivity().getSharedPreferences("LanguageSettings", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLanguageSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get current language
        val currentLang = prefs.getString("language", "en") ?: "en"

        // Set the appropriate radio button
        if (currentLang == "en") {
            binding.radioEnglish.isChecked = true
        } else {
            binding.radioVietnamese.isChecked = true
        }

        // Setup language selection
        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedLanguage = if (checkedId == R.id.radioEnglish) "en" else "vi"
            setAppLanguage(selectedLanguage)
        }

        // Setup save button
        binding.btnSave.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun setAppLanguage(langCode: String) {
        // Save language preference
        prefs.edit().putString("language", langCode).apply()

        // Update app locale
        val locale = Locale(langCode)
        Locale.setDefault(locale)

        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        // Show confirmation
        val message = if (langCode == "en") "Language changed to English" else "Đã chuyển sang tiếng Việt"
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

        // To apply changes immediately, recreate the activity
        requireActivity().recreate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = LanguageSettingsFragment()
    }
}