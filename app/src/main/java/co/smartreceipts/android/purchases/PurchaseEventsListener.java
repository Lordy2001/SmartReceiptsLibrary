package co.smartreceipts.android.purchases;

import android.support.annotation.NonNull;

import java.util.List;

import co.smartreceipts.android.purchases.model.InAppPurchase;
import co.smartreceipts.android.purchases.source.PurchaseSource;
import co.smartreceipts.android.purchases.wallet.PurchaseWallet;

public interface PurchaseEventsListener {

    /**
     * Called if we successfully completed a purchase
     *
     * @param inAppPurchase the new subscription that we purchased
     * @param purchaseSource where the purchase flow was initiated
     */
    void onPurchaseSuccess(@NonNull InAppPurchase inAppPurchase, @NonNull PurchaseSource purchaseSource);

    /**
     * Called if we failed to complete a purchase
     *
     * @param purchaseSource where the purchase flow was initiated
     */
    void onPurchaseFailed(@NonNull PurchaseSource purchaseSource);

}
