package io.horizontalsystems.bankwallet.modules.swap.confirmation

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.ethereum.EthereumFeeViewModel
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmData
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmModule
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmModule.additionalInfoKey
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmModule.transactionDataKey
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.SwapMainViewModel
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.snackbar.CustomSnackbar
import io.horizontalsystems.snackbar.SnackbarDuration
import kotlinx.android.synthetic.main.fragment_confirmation_swap.*

abstract class BaseSwapConfirmationFragment : BaseFragment() {

    protected abstract val logger: AppLogger
    protected abstract val sendViewModel: SendEvmTransactionViewModel
    protected abstract val feeViewModel: EthereumFeeViewModel

    private val mainViewModel by navGraphViewModels<SwapMainViewModel>(R.id.swapFragment)
    protected val dex: SwapMainModule.Dex
        get() = mainViewModel.dex

    private var snackbarInProcess: CustomSnackbar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_confirmation_swap, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuCancel -> {
                    findNavController().popBackStack()
                    true
                }
                else -> false
            }
        }

        sendViewModel.sendEnabledLiveData.observe(viewLifecycleOwner, { enabled ->
            swapButton.isEnabled = enabled
        })

        sendViewModel.sendingLiveData.observe(viewLifecycleOwner, {
            snackbarInProcess = HudHelper.showInProcessMessage(requireView(), R.string.Swap_Swapping, SnackbarDuration.INDEFINITE)
        })

        sendViewModel.sendSuccessLiveData.observe(viewLifecycleOwner, { transactionHash ->
            HudHelper.showSuccessMessage(requireActivity().findViewById(android.R.id.content), R.string.Hud_Text_Success)
            Handler(Looper.getMainLooper()).postDelayed({
                findNavController().popBackStack(R.id.swapFragment, true)
            }, 1200)
        })

        sendViewModel.sendFailedLiveData.observe(viewLifecycleOwner, {
            HudHelper.showErrorMessage(requireActivity().findViewById(android.R.id.content), it)

            findNavController().popBackStack()
        })

        sendEvmTransactionView.init(
                sendViewModel,
                feeViewModel,
                viewLifecycleOwner,
                parentFragmentManager,
                showSpeedInfoListener = {
                    findNavController().navigate(R.id.swapConfirmationFragment_to_feeSpeedInfo, null, navOptions())
                }
        )

        swapButton.setOnSingleClickListener {
            logger.info("click swap button")
            sendViewModel.send(logger)
        }
    }

    override fun onDestroyView() {
        snackbarInProcess?.dismiss()
        super.onDestroyView()
    }

}