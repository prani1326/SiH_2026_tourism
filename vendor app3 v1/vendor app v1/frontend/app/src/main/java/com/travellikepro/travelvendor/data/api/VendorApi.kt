package com.travellikepro.travelvendor.data.api

import com.travellikepro.travelvendor.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface VendorApi {

    // Auth
    @POST("api/vendor/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): ApiResponse<LoginData>

    @POST("api/vendor/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): ApiResponse<LoginData>

    @GET("api/vendor/auth/me")
    suspend fun getMe(): ApiResponse<Vendor>

    // Profile
    @GET("api/vendor/profile")
    suspend fun getProfile(): ApiResponse<Vendor>

    @PUT("api/vendor/profile")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): ApiResponse<Vendor>

    @Multipart
    @POST("api/vendor/profile/photo")
    suspend fun uploadProfilePhoto(
        @Part profilePhoto: MultipartBody.Part
    ): ApiResponse<UploadPhotoResponse>

    // Dashboard
    @GET("api/vendor/dashboard")
    suspend fun getDashboard(): ApiResponse<DashboardData>

    // KYC
    @GET("api/vendor/kyc")
    suspend fun getKyc(): ApiResponse<KycResponseData>

    @POST("api/vendor/kyc")
    suspend fun submitKyc(
        @Body request: KycSubmitRequest
    ): ApiResponse<KycDetails>

    @PUT("api/vendor/kyc")
    suspend fun updateKyc(
        @Body request: KycSubmitRequest
    ): ApiResponse<KycDetails>

    @Multipart
    @POST("api/vendor/kyc/documents")
    suspend fun uploadKycDocument(
        @Part document: MultipartBody.Part,
        @Part("doc_type") docType: RequestBody
    ): ApiResponse<KycDocument>

    @GET("api/vendor/kyc/status")
    suspend fun getKycStatus(): ApiResponse<KycResponseData>

    // Listings
    @GET("api/vendor/listings")
    suspend fun getListings(
        @Query("status") status: String? = null,
        @Query("search") search: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiResponse<ListingsResponseData>

    @GET("api/vendor/listings/{id}")
    suspend fun getListingById(
        @Path("id") id: Int
    ): ApiResponse<Listing>

    @POST("api/vendor/listings")
    suspend fun createListing(
        @Body request: CreateListingRequest
    ): ApiResponse<Listing>

    @PUT("api/vendor/listings/{id}")
    suspend fun updateListing(
        @Path("id") id: Int,
        @Body request: CreateListingRequest
    ): ApiResponse<Listing>

    @DELETE("api/vendor/listings/{id}")
    suspend fun deleteListing(
        @Path("id") id: Int
    ): ApiResponse<Any>

    // Bookings
    @GET("api/vendor/bookings")
    suspend fun getBookings(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiResponse<BookingsResponseData>

    @GET("api/vendor/bookings/{id}")
    suspend fun getBookingById(
        @Path("id") id: Int
    ): ApiResponse<Booking>

    @POST("api/vendor/bookings/{id}/accept")
    suspend fun acceptBooking(
        @Path("id") id: Int
    ): ApiResponse<Any>

    @POST("api/vendor/bookings/{id}/reject")
    suspend fun rejectBooking(
        @Path("id") id: Int,
        @Body request: RejectBookingRequest
    ): ApiResponse<Any>

    // Wallet
    @GET("api/vendor/wallet")
    suspend fun getWallet(): ApiResponse<WalletSummary>

    @GET("api/vendor/wallet/transactions")
    suspend fun getTransactions(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("type") type: String? = null
    ): ApiResponse<TransactionsResponseData>

    // Notifications
    @GET("api/vendor/notifications")
    suspend fun getNotifications(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("unread") unreadOnly: Boolean = false
    ): ApiResponse<NotificationsResponseData>

    @POST("api/vendor/notifications/{id}/read")
    suspend fun markNotificationAsRead(
        @Path("id") id: Int
    ): ApiResponse<Any>

    @POST("api/vendor/notifications/read-all")
    suspend fun markAllNotificationsAsRead(): ApiResponse<Any>
}