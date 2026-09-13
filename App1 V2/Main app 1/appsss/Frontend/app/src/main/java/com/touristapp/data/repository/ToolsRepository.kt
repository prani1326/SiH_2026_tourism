package com.touristapp.data.repository

import com.touristapp.data.remote.BackendApiClient
import com.touristapp.data.remote.model.MenuScanResponseDto
import com.touristapp.data.remote.model.SimPlanDto
import com.touristapp.data.remote.model.TranslationResponseDto
import com.touristapp.data.remote.model.VisaDocumentDto

class ToolsRepository(
    private val backendApiClient: BackendApiClient = BackendApiClient()
) {
    suspend fun getSimPlans(): Result<List<SimPlanDto>> {
        val res = backendApiClient.getSimPlans()
        if (res.isSuccess) return res

        return Result.success(
            listOf(
                SimPlanDto("sim_1", "Jio Tourist Unlimited", "2GB/day (High-Speed)", 28, 349.0, true),
                SimPlanDto("sim_2", "Airtel India Roam", "1.5GB/day + Unlimited Calls", 28, 299.0, true),
                SimPlanDto("sim_3", "Vi Tourist International Pass", "3GB/day + 100 SMS", 14, 499.0, true)
            )
        )
    }

    suspend fun purchaseSimPlan(planId: String): Result<String> {
        return backendApiClient.purchaseSimPlan(planId)
    }

    suspend fun getVisaDocuments(): Result<List<VisaDocumentDto>> {
        val res = backendApiClient.getVisaDocuments()
        if (res.isSuccess) return res

        return Result.success(
            listOf(
                VisaDocumentDto("doc_1", "Indian Tourist e-Visa", "India", "Active / Valid", "https://indianvisaonline.gov.in"),
                VisaDocumentDto("doc_2", "Passport Copy (Encrypted)", "India", "Valid until 2032", "vault://docs/passport_sample.pdf")
            )
        )
    }

    suspend fun translateText(text: String, targetLang: String): Result<TranslationResponseDto> {
        return backendApiClient.translateText(text, targetLang)
    }

    suspend fun scanMenu(menuText: String, diet: String): Result<MenuScanResponseDto> {
        return backendApiClient.scanMenu(menuText, diet)
    }

    suspend fun getInsurancePolicies(): Result<List<com.touristapp.data.remote.model.InsurancePolicyDto>> {
        val res = backendApiClient.getInsurancePolicies()
        if (res.isSuccess) return res
        return Result.success(
            listOf(
                com.touristapp.data.remote.model.InsurancePolicyDto("ins_1", "Bajaj Allianz", "Medical (₹5L), Trip Cancellation, Baggage Delay", 599.0, 500000.0),
                com.touristapp.data.remote.model.InsurancePolicyDto("ins_2", "HDFC ERGO", "Medical (₹10L), Accident, Loss of Passport", 799.0, 1000000.0),
                com.touristapp.data.remote.model.InsurancePolicyDto("ins_3", "Care Health Explorer", "Emergency Evacuation, OPD & IPD Coverage", 999.0, 1500000.0)
            )
        )
    }

    suspend fun purchaseInsurancePolicy(policyId: String): Result<String> {
        return backendApiClient.purchaseInsurancePolicy(policyId)
    }

    suspend fun getPreparationChecklist(destination: String = "Jaipur"): Result<com.touristapp.data.remote.model.PreparationChecklistDto> {
        val res = backendApiClient.getPreparationChecklist(destination)
        if (res.isSuccess) return res
        return Result.success(
            com.touristapp.data.remote.model.PreparationChecklistDto(
                destination = destination,
                weatherPackingList = listOf(
                    com.touristapp.data.remote.model.WeatherPackingItemDto("Breathable cotton clothes", false, "Hot/Sunny days"),
                    com.touristapp.data.remote.model.WeatherPackingItemDto("Cushioned walking shoes", false, "Cobblestone palace walking"),
                    com.touristapp.data.remote.model.WeatherPackingItemDto("Sunscreen SPF 50+ & Sunglasses", false, "High UV heritage sites"),
                    com.touristapp.data.remote.model.WeatherPackingItemDto("Refillable water bottle", false, "Hydration")
                ),
                culturalPreparation = listOf(
                    com.touristapp.data.remote.model.CulturalEtiquetteDto("Temple Attire", "Cover shoulders & knees. Carry scarf for head covering."),
                    com.touristapp.data.remote.model.CulturalEtiquetteDto("Shoe Removal", "Remove shoes before entering sanctums."),
                    com.touristapp.data.remote.model.CulturalEtiquetteDto("Bargaining", "Friendly polite negotiation in local bazaars.")
                ),
                travelMedicalKit = listOf(
                    com.touristapp.data.remote.model.MedicalKitItemDto("ORS Sachets", "Electrolyte hydration"),
                    com.touristapp.data.remote.model.MedicalKitItemDto("Antacid / Digestive Tablets", "Spicy food transition"),
                    com.touristapp.data.remote.model.MedicalKitItemDto("Band-aids & Antiseptic", "Foot blister prevention")
                ),
                internationalChecklist = listOf(
                    com.touristapp.data.remote.model.InternationalChecklistItemDto("Power Adapter Type D / C (230V)", true),
                    com.touristapp.data.remote.model.InternationalChecklistItemDto("Passport & e-Visa Offline Copy", true)
                )
            )
        )
    }

    suspend fun compareTransport(city: String = "Agra"): Result<com.touristapp.data.remote.model.TransportComparisonResponseDto> {
        val res = backendApiClient.compareTransport(city)
        if (res.isSuccess) return res
        return Result.success(
            com.touristapp.data.remote.model.TransportComparisonResponseDto(
                city = city,
                route = "$city Airport → City Center / Hotel Hub",
                options = listOf(
                    com.touristapp.data.remote.model.TransportOptionDto("Airport Express Metro", "Cheapest & Eco", 25, 60.0, 4.9, "Low", "04:45 AM - 11:30 PM", "Luggage racks available"),
                    com.touristapp.data.remote.model.TransportOptionDto("Prepaid Govt Taxi", "Safest Fixed Price", 45, 650.0, 4.8, "Instant Pickup", "24/7", "Collect slip inside terminal"),
                    com.touristapp.data.remote.model.TransportOptionDto("App Cab (Uber/Ola)", "Fastest Door-to-Door", 40, 520.0, 4.7, "Pillar pickup", "24/7", "Follow floor signs to App Cab floor")
                )
            )
        )
    }
}

