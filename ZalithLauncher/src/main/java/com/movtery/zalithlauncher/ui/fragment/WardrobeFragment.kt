package com.movtery.zalithlauncher.ui.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import com.movtery.anim.AnimPlayer
import com.movtery.anim.animations.Animations
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.databinding.FragmentWardrobeBinding
import com.movtery.zalithlauncher.feature.accounts.AccountUtils
import com.movtery.zalithlauncher.feature.accounts.AccountsManager
import com.movtery.zalithlauncher.utils.ZHTools
import com.movtery.zalithlauncher.utils.path.PathManager
import com.movtery.zalithlauncher.utils.skin.SkinFileValidator
import net.kdt.pojavlaunch.value.MinecraftAccount
import java.io.File
import java.io.FileOutputStream

class WardrobeFragment : FragmentWithAnim(R.layout.fragment_wardrobe), View.OnClickListener {
    companion object {
        const val TAG = "WardrobeFragment"
    }

    private lateinit var binding: FragmentWardrobeBinding

    private val pickSkinLauncher = registerForActivityResult(OpenDocument()) { uri -> uri?.let(::importSkin) }
    private val pickCapeLauncher = registerForActivityResult(OpenDocument()) { uri -> uri?.let(::importCape) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentWardrobeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            refreshSkinButton.setOnClickListener(this@WardrobeFragment)
            refreshCapeButton.setOnClickListener(this@WardrobeFragment)
            importSkinButton.setOnClickListener(this@WardrobeFragment)
            importCapeButton.setOnClickListener(this@WardrobeFragment)
            clearWardrobeButton.setOnClickListener(this@WardrobeFragment)
            backButton.setOnClickListener(this@WardrobeFragment)
        }

        updateUI()
    }

    override fun onClick(v: View) {
        val currentAccount = AccountsManager.currentAccount
        if (currentAccount == null) {
            Toast.makeText(requireContext(), R.string.account_wardrobe_no_account, Toast.LENGTH_LONG).show()
            return
        }

        when (v.id) {
            R.id.refresh_skin_button -> {
                refreshSkin(currentAccount)
            }
            R.id.refresh_cape_button -> {
                refreshCape(currentAccount)
            }
            R.id.import_skin_button -> {
                pickSkinLauncher.launch(arrayOf("image/png", "image/*"))
            }
            R.id.import_cape_button -> {
                pickCapeLauncher.launch(arrayOf("image/png", "image/*"))
            }
            R.id.clear_wardrobe_button -> {
                clearWardrobe(currentAccount)
            }
            R.id.back_button -> {
                ZHTools.onBackPressed(requireActivity())
            }
        }
    }

    private fun updateUI() {
        val account = AccountsManager.currentAccount
        if (account == null) {
            binding.accountName.text = getString(R.string.account_wardrobe_no_account)
            binding.accountType.visibility = View.GONE
            binding.wardrobeStatus.text = ""
            binding.refreshSkinButton.isEnabled = false
            binding.refreshCapeButton.isEnabled = false
            binding.importSkinButton.isEnabled = false
            binding.importCapeButton.isEnabled = false
            binding.clearWardrobeButton.isEnabled = false
            return
        }

        val currentSkin = account.getSkinFile().exists()
        val currentCape = account.getCapeFile().exists()
        val isPremium = AccountUtils.isMicrosoftAccount(account) || AccountUtils.isOtherLoginAccount(account)

        binding.accountName.text = getString(R.string.account_wardrobe_current_account, account.username)
        binding.accountType.visibility = View.VISIBLE
        binding.accountType.text = AccountUtils.getAccountTypeName(requireContext(), account)
        binding.wardrobeStatus.text = getString(
            R.string.account_wardrobe_status,
            if (currentSkin) getString(R.string.account_wardrobe_skin_cached) else getString(R.string.account_wardrobe_skin_missing),
            if (currentCape) getString(R.string.account_wardrobe_cape_cached) else getString(R.string.account_wardrobe_cape_missing)
        )
        binding.refreshSkinButton.isEnabled = isPremium
        binding.refreshCapeButton.isEnabled = isPremium
        binding.importSkinButton.isEnabled = true
        binding.importCapeButton.isEnabled = true
        binding.clearWardrobeButton.isEnabled = currentSkin || currentCape
    }

    private fun refreshSkin(account: MinecraftAccount) {
        if (AccountUtils.isMicrosoftAccount(account)) {
            account.updateMicrosoftSkin()
            Toast.makeText(requireContext(), R.string.account_wardrobe_refresh_skin_done, Toast.LENGTH_SHORT).show()
        } else if (AccountUtils.isOtherLoginAccount(account)) {
            account.updateOtherSkin()
            Toast.makeText(requireContext(), R.string.account_wardrobe_refresh_skin_done, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), R.string.account_wardrobe_no_account, Toast.LENGTH_LONG).show()
        }
        updateUI()
    }

    private fun refreshCape(account: MinecraftAccount) {
        if (AccountUtils.isMicrosoftAccount(account)) {
            account.updateMicrosoftCape()
            Toast.makeText(requireContext(), R.string.account_wardrobe_refresh_cape_done, Toast.LENGTH_SHORT).show()
        } else if (AccountUtils.isOtherLoginAccount(account)) {
            account.updateOtherCape()
            Toast.makeText(requireContext(), R.string.account_wardrobe_refresh_cape_done, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), R.string.account_wardrobe_no_account, Toast.LENGTH_LONG).show()
        }
        updateUI()
    }

    private fun importSkin(uri: Uri) {
        importFile(uri, true)
    }

    private fun importCape(uri: Uri) {
        importFile(uri, false)
    }

    private fun clearWardrobe(account: MinecraftAccount) {
        val skinFile = account.getSkinFile()
        val capeFile = account.getCapeFile()
        if (skinFile.exists()) skinFile.delete()
        if (capeFile.exists()) capeFile.delete()
        Toast.makeText(requireContext(), R.string.account_wardrobe_import_success_skin, Toast.LENGTH_SHORT).show()
        updateUI()
    }

    private fun importFile(uri: Uri, skin: Boolean) {
        val account = AccountsManager.currentAccount ?: return
        val targetFile = if (skin) account.getSkinFile() else account.getCapeFile()
        if (!saveUriToFile(uri, targetFile)) {
            Toast.makeText(requireContext(), R.string.account_wardrobe_import_failed, Toast.LENGTH_LONG).show()
            return
        }

        val valid = if (skin) SkinFileValidator.isValidSkinFile(targetFile) else SkinFileValidator.isValidCapeFile(targetFile)
        if (!valid) {
            targetFile.delete()
            Toast.makeText(requireContext(), R.string.account_wardrobe_import_failed, Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(
            requireContext(),
            if (skin) R.string.account_wardrobe_import_success_skin else R.string.account_wardrobe_import_success_cape,
            Toast.LENGTH_SHORT
        ).show()
        updateUI()
    }

    private fun saveUriToFile(uri: Uri, target: File): Boolean {
        return try {
            target.parentFile?.mkdirs()
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        binding.root.let {
            animPlayer.apply(AnimPlayer.Entry(it, Animations.BounceInDown))
        }
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        binding.root.let {
            animPlayer.apply(AnimPlayer.Entry(it, Animations.FadeOutRight))
        }
    }
}
