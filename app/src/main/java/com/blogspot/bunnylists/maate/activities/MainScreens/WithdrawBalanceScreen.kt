package com.blogspot.bunnylists.maate.activities.MainScreens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.blogspot.bunnylists.maate.R
import com.blogspot.bunnylists.maate.Utils.MyApplication
import com.blogspot.bunnylists.maate.Utils.NetworkResult
import com.blogspot.bunnylists.maate.databinding.FragmentWithdrawBalanceScreenBinding
import com.blogspot.bunnylists.maate.models.LoadingDialog
import com.blogspot.bunnylists.maate.viewModels.MainScreenViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.rpc.context.AttributeContext
import java.text.SimpleDateFormat
import java.util.*

class WithdrawBalanceScreen : Fragment() {


    private val mViewModel: MainScreenViewModel by activityViewModels() {
        MainScreenViewModel.provideFactory((requireActivity().application as MyApplication).repository, this)
    }
    private lateinit var bind : FragmentWithdrawBalanceScreenBinding
    private lateinit var loadingDialog: LoadingDialog
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        bind = FragmentWithdrawBalanceScreenBinding.inflate(inflater, container, false)
        loadingDialog = LoadingDialog(requireActivity())
        mViewModel.checkWithdrawRequest()
        setActionListners()

        return bind.root
    }

    private fun setActionListners() {
        bind.cancelButton.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.pop_enter, R.anim.pop_exit, R.anim.enter, R.anim.exit)
                .replace(R.id.fragment_container, ProfileScreen())
                .commit()
        }

        bind.withdrawButton.setOnClickListener {
            if (bind.amountField.text.isNullOrBlank()
                || bind.nameField.text.isNullOrBlank()
                || bind.accountNumberField.text.isNullOrBlank()
                || bind.ifscField.text.isNullOrBlank()
            )
                Toast.makeText(requireContext(), "Enter all details!", Toast.LENGTH_SHORT).show()
            else {
                mViewModel.amount = bind.amountField.text.toString().toInt()
                mViewModel.accountName = bind.nameField.text.toString()
                mViewModel.accountNumber = bind.accountNumberField.text.toString().toLong()
                mViewModel.ifscCode = bind.ifscField.text.toString()
                mViewModel.withdrawBalance()
            }
        }

        bind.helpButton.setOnClickListener {
            val request = mViewModel.haveWithdrawRequest.value!!
            val intent = Intent(Intent.ACTION_SEND)
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("\"care.livePari@gmail.com\""))
            intent.putExtra(Intent.EXTRA_SUBJECT, "Regarding Payment #${request.requestId} of #${request.uid}")
            intent.type = "message/rfc822"
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                intent.setPackage("com.google.android.gm")
                startActivity(intent)
            } else {
                startActivity(Intent.createChooser(intent, "Please choose a mail client"))
            }
        }

        bind.editButton.setOnClickListener {
            val request = mViewModel.haveWithdrawRequest.value!!
            mViewModel.haveWithdrawRequest.removeObservers(viewLifecycleOwner)
            bind.requestCard.isVisible=false
            bind.accountNumberLayout.isVisible = true
            bind.accountNumberField.setText(request.acNumber.toString())
            bind.amountLayout.isVisible = true
            bind.amountField.setText(request.amount.toString())
            bind.nameLayout.isVisible = true
            bind.nameField.setText(request.acName.toString())
            bind.ifscLayout.isVisible = true
            bind.ifscField.setText(request.ifsc.toString())
            bind.requestButtons.isVisible = false
            bind.sendingButtons.isVisible = true
        }

        bind.deleteButton.setOnClickListener {
            mViewModel.removeWithdrawRequest()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel.user.observe(requireActivity()){
            bind.user = it
        }

        mViewModel.haveWithdrawRequest.observe(viewLifecycleOwner){
            if(it!=null){
                bind.accountNumberLayout.isVisible = false
                bind.amountLayout.isVisible = false
                bind.nameLayout.isVisible = false
                bind.ifscLayout.isVisible = false
                bind.requestCard.isVisible = true
                bind.requestButtons.isVisible = true
                bind.sendingButtons.isVisible = false
                bind.withdrawNote.text = "Note: You have an active request!\n Please wait or modify this request"
                bind.withdrawNote.setTextColor(Color.parseColor("#F75555"))
                bind.amountText.text = "₹ ${it.amount.toString()}"
                bind.statusText.text = it.status
                bind.timeText.text = it.time
                bind.acText.text = it.acNumber.toString()
                bind.ifscText.text = it.ifsc.toString()
            }else{
                bind.accountNumberLayout.isVisible = true
                bind.amountLayout.isVisible = true
                bind.nameLayout.isVisible = true
                bind.ifscLayout.isVisible = true
                bind.requestCard.isVisible = false
                bind.requestButtons.isVisible = false
                bind.sendingButtons.isVisible = true
                bind.withdrawNote.text = "Note: The amount will be send to your account within next 6 Working Hours!"
                bind.withdrawNote.setTextColor(Color.parseColor("#9E9E9E"))
            }
        }

        mViewModel.transactionTask.observe(viewLifecycleOwner){
            when(it){
                is NetworkResult.Loading -> {
                    loadingDialog.startLoading()
                }
                is NetworkResult.Success -> {
                    loadingDialog.isDismiss()
                    mViewModel.resetTransactionTask()
                    mViewModel.clearWithdrawDetails()
                    if(!it.massage.isNullOrBlank()) {
                        Toast.makeText(requireContext(), it.massage, Toast.LENGTH_SHORT).show()
                    }
                    requireActivity().supportFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.pop_enter, R.anim.pop_exit, R.anim.enter, R.anim.exit)
                        .replace(R.id.fragment_container, ProfileScreen())
                        .commit()
                }
                is NetworkResult.Error -> {
                    loadingDialog.isDismiss()
                    mViewModel.resetTransactionTask()
                    mViewModel.clearWithdrawDetails()
                    if(!it.massage.isNullOrBlank()) {
                        Toast.makeText(requireContext(), it.massage, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

}