package com.travellikepro.travelvendor.data.repository

import android.content.Context
import com.travellikepro.travelvendor.data.api.RetrofitClient
import com.travellikepro.travelvendor.data.model.CreateListingRequest

class ListingsRepository(context: Context) {
    private val api = RetrofitClient.getApi(context)

    suspend fun getListings(status: String? = null, search: String? = null) =
        api.getListings(status = status, search = search)

    suspend fun getListingById(id: Int) = api.getListingById(id)

    suspend fun createListing(request: CreateListingRequest) = api.createListing(request)

    suspend fun updateListing(id: Int, request: CreateListingRequest) = api.updateListing(id, request)

    suspend fun deleteListing(id: Int) = api.deleteListing(id)
}
