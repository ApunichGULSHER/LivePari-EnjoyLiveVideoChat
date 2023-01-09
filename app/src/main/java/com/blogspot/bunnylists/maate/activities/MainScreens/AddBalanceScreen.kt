package com.blogspot.bunnylists.maate.activities.MainScreens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.blogspot.bunnylists.maate.R
import com.blogspot.bunnylists.maate.Utils.MyApplication
import com.blogspot.bunnylists.maate.Utils.NetworkResult
import com.blogspot.bunnylists.maate.databinding.FragmentAddBalanceScreenBinding
import com.blogspot.bunnylists.maate.models.LoadingDialog
import com.blogspot.bunnylists.maate.viewModels.MainScreenViewModel
import com.razorpay.Checkout
import org.json.JSONObject


class AddBalanceScreen : Fragment() {
    private val mViewModel: MainScreenViewModel by activityViewModels() {
        MainScreenViewModel.provideFactory(
            (requireActivity().application as MyApplication).repository,
            this
        )
    }
    private lateinit var bind: FragmentAddBalanceScreenBinding
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var co: Checkout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        bind = FragmentAddBalanceScreenBinding.inflate(inflater, container, false)
        loadingDialog = LoadingDialog(requireActivity())

        bind.cancelButton.setOnClickListener {
            val prevFrag = ProfileScreen()
            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.pop_enter, R.anim.pop_exit, R.anim.enter, R.anim.exit)
                .replace(R.id.fragment_container, prevFrag)
                .commit()
        }

        bind.addButton.setOnClickListener {
            if (bind.textField.text.isNullOrBlank())
                Toast.makeText(requireContext(), "Enter amount", Toast.LENGTH_SHORT).show()
            else {
                mViewModel.amount = bind.textField.text.toString().toInt() * 100
                co = Checkout()
                mViewModel.initiatePayment(co)

            }
        }

        return bind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel.user.observe(requireActivity()) {
            bind.user = it
        }

        mViewModel.paymentInitTask.observe(viewLifecycleOwner) {
            when (it) {
                is NetworkResult.Loading -> {
                    loadingDialog.startLoading()
                }
                is NetworkResult.Success -> {
                    loadingDialog.isDismiss()
                    co.open(requireActivity(), it.data as JSONObject)
                    mViewModel.resetInitTask()
                }
                is NetworkResult.Error -> {
                    loadingDialog.isDismiss()
                    if (!it.massage.isNullOrBlank()) {
                        Toast.makeText(requireContext(), it.massage, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        mViewModel.resetBalanceTask.observe(viewLifecycleOwner) {
            when (it) {
                is NetworkResult.Loading -> {
                    loadingDialog.startLoading()
                }
                is NetworkResult.Success -> {
                    loadingDialog.isDismiss()
                    mViewModel.amount = null
                    mViewModel.resetTask()
                    requireActivity().supportFragmentManager.beginTransaction()
                        .setCustomAnimations(
                            R.anim.pop_enter,
                            R.anim.pop_exit,
                            R.anim.enter,
                            R.anim.exit
                        )
                        .replace(R.id.fragment_container, ProfileScreen())
                        .commit()
                }
                is NetworkResult.Error -> {
                    loadingDialog.isDismiss()
                    mViewModel.amount = null
                    if (!it.massage.isNullOrBlank()) {
                        Toast.makeText(requireContext(), it.massage, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

}