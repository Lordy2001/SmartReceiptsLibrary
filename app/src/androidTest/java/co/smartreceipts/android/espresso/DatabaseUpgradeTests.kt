package co.smartreceipts.android.espresso

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import co.smartreceipts.android.SmartReceiptsApplication
import co.smartreceipts.android.activities.SmartReceiptsActivity
import co.smartreceipts.android.espresso.test.runner.BeforeApplicationOnCreate
import co.smartreceipts.android.espresso.test.utils.TestLocaleToggler
import co.smartreceipts.android.espresso.test.utils.TestResourceReader
import co.smartreceipts.android.model.*
import co.smartreceipts.android.model.impl.columns.receipts.ReceiptColumnDefinitions
import co.smartreceipts.android.persistence.DatabaseHelper
import org.apache.commons.io.IOUtils
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class DatabaseUpgradeTests {

    companion object {

        @Suppress("unused")
        @JvmStatic
        @BeforeApplicationOnCreate
        fun setUpBeforeApplicationOnCreate() {
            Log.i("DatabaseUpgradeTests", "Copying our test v15 database onto the local device...")

            // Set up the database info for the source and destination
            val inputStream = TestResourceReader().openStream(TestResourceReader.DATABASE_V15)
            val databaseLocation = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), DatabaseHelper.DATABASE_NAME)

            // Copy our test from from our resources folder to the device
            val outputStream = FileOutputStream(databaseLocation)
            IOUtils.copy(inputStream, outputStream)
            IOUtils.closeQuietly(inputStream)
            IOUtils.closeQuietly(outputStream)
        }

    }

    @Rule
    @JvmField
    val activityTestRule = ActivityTestRule(SmartReceiptsActivity::class.java)

    private lateinit var databaseHelper: DatabaseHelper

    private lateinit var context: Context

    @Before
    fun setUp() {
        val application = activityTestRule.activity.application as SmartReceiptsApplication
        databaseHelper = application.databaseHelper

        // Set the Locale to en-US for consistency purposes
        val nonLocalizedContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = nonLocalizedContext.resources.configuration
        configuration.setLocale(Locale.US)
        TestLocaleToggler.setDefaultLocale(Locale.US)
        context = nonLocalizedContext.createConfigurationContext(configuration)
    }

    @After
    fun tearDown() {
        TestLocaleToggler.resetDefaultLocale()
    }

    /**
     * This test runs through an old test database, so it simple exists to verify that we were able
     * to properly upgrade and that no data was lost in the process. It should be further noted that
     * while the database contains a number of receipts, most of them are copies of each other. As
     * such, we perform our validation of the receipts using one of three methods below:
     * <ul>
     *  <li>[verifyPictureReceipt]</li>
     *  <li>[verifyFullPictureReceipt]</li>
     *  <li>[verifyPdfSampleReceipt]</li>
     * </ul>
     */
    @Test
    fun upgradeFromV15() {
        // TODO: Instead of sleep, use an idling resource that triggers once #onUpgrade is complete
        Thread.sleep(TimeUnit.SECONDS.toMillis(7)) // Wait a few seconds to ensure the database loads

        // First - confirm that we're on the latest database version
        assertEquals(DatabaseHelper.DATABASE_VERSION, databaseHelper.readableDatabase.version)

        // Next - verify each of our categories
        var categoryId = 0
        val categories = databaseHelper.categoriesTable.get().blockingGet()
        verifyCategory(categories[categoryId++], "<Category>", "NUL", 0)
        verifyCategory(categories[categoryId++], "Airfare", "AIRP", 0)
        verifyCategory(categories[categoryId++], "Books/Periodicals", "ZBKP", 0)
        verifyCategory(categories[categoryId++], "Breakfast", "BRFT", 0)
        verifyCategory(categories[categoryId++], "Car Rental", "RCAR", 0)
        verifyCategory(categories[categoryId++], "Cell Phone", "ZCEL", 0)
        val dinnerCategory = categories[categoryId++]
        verifyCategory(dinnerCategory, "Dinner", "DINN", 0)
        verifyCategory(categories[categoryId++], "Dues/Subscriptions", "ZDUE", 0)
        verifyCategory(categories[categoryId++], "Entertainment", "ENT", 0)
        verifyCategory(categories[categoryId++], "Gasoline", "GAS", 0)
        verifyCategory(categories[categoryId++], "Gift","GIFT", 0)
        verifyCategory(categories[categoryId++], "Hotel", "HTL", 0)
        verifyCategory(categories[categoryId++], "Laundry","LAUN", 0)
        val lunchCategory = categories[categoryId++]
        verifyCategory(lunchCategory, "Lunch", "LNCH", 0)
        verifyCategory(categories[categoryId++], "Meals (Justified)", "ZMEO", 0)
        verifyCategory(categories[categoryId++], "Other", "MISC", 0)
        verifyCategory(categories[categoryId++], "Parking/Tolls", "PARK", 0)
        verifyCategory(categories[categoryId++], "Postage/Shipping", "POST", 0)
        verifyCategory(categories[categoryId++], "Stationery/Stations", "ZSTS", 0)
        verifyCategory(categories[categoryId++], "Taxi/Bus", "TAXI", 0)
        verifyCategory(categories[categoryId++], "Telephone/Fax", "TELE", 0)
        verifyCategory(categories[categoryId++], "Tip", "TIP", 0)
        verifyCategory(categories[categoryId++], "Train", "TRN", 0)
        verifyCategory(categories[categoryId], "Training Fees", "ZTRN", 0)

        // Next - verify each of our payment methods
        val paymentMethods = databaseHelper.paymentMethodsTable.get().blockingGet()
        verifyPaymentMethod(paymentMethods[0], 1, "Unspecified", 0)
        verifyPaymentMethod(paymentMethods[1], 2, "Corporate Card", 0)
        verifyPaymentMethod(paymentMethods[2], 3, "Personal Card", 0)
        verifyPaymentMethod(paymentMethods[3], 4, "Check", 0)
        verifyPaymentMethod(paymentMethods[4], 5, "Cash", 0)

        // Next - verify each of our CSV columns
        val csvColumns = databaseHelper.csvTable.get().blockingGet()
        verifyCsvColumns(csvColumns[0], 1, ReceiptColumnDefinitions.ActualDefinition.CATEGORY_CODE, 0)
        verifyCsvColumns(csvColumns[1], 2, ReceiptColumnDefinitions.ActualDefinition.NAME, 0)
        verifyCsvColumns(csvColumns[2], 3, ReceiptColumnDefinitions.ActualDefinition.PRICE, 0)
        verifyCsvColumns(csvColumns[3], 4, ReceiptColumnDefinitions.ActualDefinition.CURRENCY, 0)
        verifyCsvColumns(csvColumns[4], 5, ReceiptColumnDefinitions.ActualDefinition.DATE, 0)

        // Next - verify each of our PDF columns
        val pdfColumns = databaseHelper.pdfTable.get().blockingGet()
        verifyPdfColumns(pdfColumns[0], 1, ReceiptColumnDefinitions.ActualDefinition.NAME, 0)
        verifyPdfColumns(pdfColumns[1], 2, ReceiptColumnDefinitions.ActualDefinition.PRICE, 0)
        verifyPdfColumns(pdfColumns[2], 3, ReceiptColumnDefinitions.ActualDefinition.DATE, 0)
        verifyPdfColumns(pdfColumns[3], 4, ReceiptColumnDefinitions.ActualDefinition.CATEGORY_NAME, 0)
        verifyPdfColumns(pdfColumns[4], 5, ReceiptColumnDefinitions.ActualDefinition.REIMBURSABLE, 0)
        verifyPdfColumns(pdfColumns[5], 6, ReceiptColumnDefinitions.ActualDefinition.PICTURED, 0)

        // Next - confirm each of our trips and the data within
        val trips = databaseHelper.tripsTable.get().blockingGet()
        assertNotNull(trips)
        assertEquals(3, trips.size)

        // Data that we'll want to store for final comparisons
        val allReceipts = mutableListOf<Receipt>()
        val allDistances = mutableListOf<Distance>()

        // Receipt counters
        var lastReceiptCustomOrderId = 0L
        var receiptIndexCounter = 1

        // Confirm the data within Report 1
        val report1 = trips[0]
        assertEquals(1, report1.id)
        assertEquals("Report 1", report1.name)
        assertEquals("Report 1", report1.directory.name)
        assertEquals("11/17/16", report1.getFormattedStartDate(context, "/"))
        assertEquals("11/20/16", report1.getFormattedEndDate(context, "/"))
        assertEquals("$45.00", report1.price.currencyFormattedPrice)
        assertEquals("USD", report1.tripCurrency.currencyCode)
        assertEquals("", report1.comment)
        assertEquals("", report1.costCenter)

        // And the receipts in report 1
        val report1Receipts = databaseHelper.receiptsTable.get(report1, false).blockingGet()
        allReceipts.addAll(report1Receipts)
        report1Receipts.forEach {
            assertEquals(receiptIndexCounter++, it.index)
            assertTrue(it.customOrderId > lastReceiptCustomOrderId) // These should be increasing for receipts
            lastReceiptCustomOrderId = it.customOrderId
            verifyPictureReceipt(it, report1, dinnerCategory) // Note: All receipts in report 1 are of this style
        }

        // And the distances
        val report1Distances = databaseHelper.distanceTable.get(report1, false).blockingGet()
        assertTrue(report1Distances.isEmpty())
        allDistances.addAll(report1Distances)

        // Confirm the data within Report 2
        val report2 = trips[1]
        assertEquals(2, report2.id)
        assertEquals("Report 2", report2.name)
        assertEquals("Report 2", report2.directory.name)
        assertEquals("11/17/16", report2.getFormattedStartDate(context, "/"))
        assertEquals("11/20/16", report2.getFormattedEndDate(context, "/"))
        assertEquals("$50.00", report2.price.currencyFormattedPrice)
        assertEquals("USD", report2.tripCurrency.currencyCode)
        assertEquals("", report2.comment)
        assertEquals("", report2.costCenter)

        // And the receipts in report 2
        receiptIndexCounter = 1
        lastReceiptCustomOrderId = 0
        val report2Receipts = databaseHelper.receiptsTable.get(report2, false).blockingGet()
        allReceipts.addAll(report2Receipts)
        report2Receipts.forEach {
            assertEquals(receiptIndexCounter++, it.index)
            assertTrue(it.customOrderId > lastReceiptCustomOrderId) // These should be increasing for receipts
            lastReceiptCustomOrderId = it.customOrderId
            when (receiptIndexCounter - 1) {
                in 1..5 -> verifyPictureReceipt(it, report2, dinnerCategory)
                in 6..8 -> verifyFullPictureReceipt(it, report2, dinnerCategory)
                else -> verifyPictureReceipt(it, report2, dinnerCategory, "11/19/16")
            }
        }

        // And the distances
        val report2Distances = databaseHelper.distanceTable.get(report2, false).blockingGet()
        assertTrue(report2Distances.isEmpty())
        allDistances.addAll(report2Distances)

        // Confirm the data within Report 3
        val report3 = trips[2]
        assertEquals(3, report3.id)
        assertEquals("Report 3", report3.name)
        assertEquals("Report 3", report3.directory.name)
        assertEquals("11/17/16", report3.getFormattedStartDate(context, "/"))
        assertEquals("11/20/16", report3.getFormattedEndDate(context, "/"))
        assertEquals("$42.00", report3.price.currencyFormattedPrice)
        assertEquals("USD", report3.tripCurrency.currencyCode)
        assertEquals("", report3.comment)
        assertEquals("", report3.costCenter)

        // And the receipts in report 3
        receiptIndexCounter = 1
        lastReceiptCustomOrderId = 0
        val report3Receipts = databaseHelper.receiptsTable.get(report3, false).blockingGet()
        allReceipts.addAll(report3Receipts)
        report3Receipts.forEach {
            assertEquals(receiptIndexCounter++, it.index)
            assertTrue(it.customOrderId > lastReceiptCustomOrderId) // These should be increasing for receipts
            lastReceiptCustomOrderId = it.customOrderId
            when (receiptIndexCounter - 1) {
                1 -> verifyPictureReceipt(it, report3, dinnerCategory)
                2 -> verifyFullPictureReceipt(it, report3, dinnerCategory)
                3 -> verifyPdfSampleReceipt(it, report3, dinnerCategory)
                else -> verifyPictureReceipt(it, report3, dinnerCategory, "11/20/16")
            }
        }

        // And the distances
        val report3Distances = databaseHelper.distanceTable.get(report3, false).blockingGet()
        assertTrue(report3Distances.isEmpty())
        allDistances.addAll(report3Distances)

        // Verify that none of our items have the same uuid
        assertNoUuidsAreEqual(categories)
        assertNoUuidsAreEqual(paymentMethods)
        assertNoUuidsAreEqual(csvColumns)
        assertNoUuidsAreEqual(pdfColumns)
        assertNoUuidsAreEqual(trips)
        assertNoUuidsAreEqual(allReceipts)
        assertNoUuidsAreEqual(allDistances)
    }

    private fun verifyCategory(category: Category, name: String, code: String, customOrderId: Long) {
        assertEquals(name, category.name)
        assertEquals(code, category.code)
        assertEquals(customOrderId, category.customOrderId)
    }

    private fun verifyPaymentMethod(paymentMethod: PaymentMethod, id: Int, name: String, customOrderId: Long) {
        assertEquals(id, paymentMethod.id)
        assertEquals(name, paymentMethod.method)
        assertEquals(customOrderId, paymentMethod.customOrderId)
    }

    private fun verifyCsvColumns(csvColumn: Column<Receipt>, id: Int, type: ReceiptColumnDefinitions.ActualDefinition, customOrderId: Long) {
        assertEquals(id, csvColumn.id)
        assertEquals(type.columnType, csvColumn.type)
        assertEquals(customOrderId, csvColumn.customOrderId)
    }

    private fun verifyPdfColumns(pdfColumn: Column<Receipt>, id: Int, type: ReceiptColumnDefinitions.ActualDefinition, customOrderId: Long) {
        assertEquals(id, pdfColumn.id)
        assertEquals(type.columnType, pdfColumn.type)
        assertEquals(customOrderId, pdfColumn.customOrderId)
    }

    private fun verifyPictureReceipt(receipt: Receipt, parent: Trip, category: Category, date: String = "11/17/16") {
        assertEquals(parent, receipt.trip)
        assertEquals("Picture", receipt.name)
        assertEquals("$5.00", receipt.price.currencyFormattedPrice)
        assertEquals("USD", receipt.price.currencyCode)
        assertEquals("$0.00", receipt.tax.currencyFormattedPrice)
        assertEquals("USD", receipt.tax.currencyCode)
        assertEquals(date, receipt.getFormattedDate(context, "/"))
        assertEquals(category, receipt.category)
        assertEquals("", receipt.comment)
        assertEquals(PaymentMethod.NONE, receipt.paymentMethod)
        assertTrue(receipt.isReimbursable)
        assertFalse(receipt.isFullPage)
    }

    private fun verifyFullPictureReceipt(receipt: Receipt, parent: Trip, category: Category, date: String = "11/18/16") {
        assertEquals(parent, receipt.trip)
        assertEquals("Full picture", receipt.name)
        assertEquals("$5.00", receipt.price.currencyFormattedPrice)
        assertEquals("USD", receipt.price.currencyCode)
        assertEquals("$0.00", receipt.tax.currencyFormattedPrice)
        assertEquals("USD", receipt.tax.currencyCode)
        assertEquals(date, receipt.getFormattedDate(context, "/"))
        assertEquals(category, receipt.category)
        assertEquals("", receipt.comment)
        assertEquals(PaymentMethod.NONE, receipt.paymentMethod)
        assertTrue(receipt.isReimbursable)
        assertTrue(receipt.isFullPage)
    }

    private fun verifyPdfSampleReceipt(receipt: Receipt, parent: Trip, category: Category, date: String = "11/19/16") {
        assertEquals(parent, receipt.trip)
        assertEquals("Pdf sample", receipt.name)
        assertEquals("$2.00", receipt.price.currencyFormattedPrice)
        assertEquals("USD", receipt.price.currencyCode)
        assertEquals("$0.00", receipt.tax.currencyFormattedPrice)
        assertEquals("USD", receipt.tax.currencyCode)
        assertEquals(date, receipt.getFormattedDate(context, "/"))
        assertEquals(category, receipt.category)
        assertEquals("", receipt.comment)
        assertEquals(PaymentMethod.NONE, receipt.paymentMethod)
        assertTrue(receipt.isReimbursable)
        assertFalse(receipt.isFullPage)
    }

    private fun assertNoUuidsAreEqual(keyedItems: List<Keyed>) {
        keyedItems.forEach { item1 ->
            keyedItems.forEach {item2 ->
                if (item1 != item2) {
                    // Don't compare if it's the same item
                    assertThat(item1.uuid, not(equalTo(item2.uuid)))
                }
            }
        }
    }

}