package com.blogspot.bunnylists.maate.activities.MainScreens

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.blogspot.bunnylists.maate.R
import com.blogspot.bunnylists.maate.Utils.MyApplication
import com.blogspot.bunnylists.maate.activities.RegisterScreens.RegisterActivity
import com.blogspot.bunnylists.maate.databinding.FragmentProfileScreenBinding
import com.blogspot.bunnylists.maate.viewModels.MainScreenViewModel
import com.google.firebase.auth.FirebaseAuth
import java.lang.System.exit

class ProfileScreen : Fragment() {

    private val mViewModel: MainScreenViewModel by activityViewModels {
        MainScreenViewModel.provideFactory((requireActivity().application as MyApplication).repository, this)
    }
    private lateinit var bind : FragmentProfileScreenBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bind = FragmentProfileScreenBinding.inflate(inflater, container, false)

        bind.addBalance.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit)
                .replace(R.id.fragment_container, AddBalanceScreen())
                .commit()
        }

        bind.withdraw.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit)
                .replace(R.id.fragment_container, WithdrawBalanceScreen())
                .commit()
        }

        bind.pricing.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit)
                .replace(R.id.fragment_container, PricingScreen())
                .commit()
        }
        bind.account.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit)
                .replace(R.id.fragment_container, AccountScreen())
                .commit()
        }

        bind.report.setOnClickListener {
            val myUid = FirebaseAuth.getInstance().currentUser!!.uid
            val intent = Intent(Intent.ACTION_SEND)
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("\"care.livePari@gmail.com\""))
            intent.putExtra(Intent.EXTRA_SUBJECT, "Facing an Issue #$myUid")
            intent.type = "message/rfc822"
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                intent.setPackage("com.google.android.gm")
                startActivity(intent)
            } else {
                startActivity(Intent.createChooser(intent, "Please choose a mail client"))
            }
        }

        return bind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel.user.observe(viewLifecycleOwner) {
            bind.user = it
        }
    }

}