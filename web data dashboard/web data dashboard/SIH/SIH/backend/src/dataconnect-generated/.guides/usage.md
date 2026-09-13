# Basic Usage

Always prioritize using a supported framework over using the generated SDK
directly. Supported frameworks simplify the developer experience and help ensure
best practices are followed.





## Advanced Usage
If a user is not using a supported framework, they can use the generated SDK directly.

Here's an example of how to use it with the first 5 operations:

```js
import { insertUser, upsertAvailability, createBooking, addProperty, addReview, updateBookingStatus, deleteReview, getMyProfile, listAllProperties, getPropertyDetails } from '@dataconnect/generated';


// Operation InsertUser: 
const { data } = await InsertUser(dataConnect);

// Operation UpsertAvailability:  For variables, look at type UpsertAvailabilityVars in ../index.d.ts
const { data } = await UpsertAvailability(dataConnect, upsertAvailabilityVars);

// Operation CreateBooking:  For variables, look at type CreateBookingVars in ../index.d.ts
const { data } = await CreateBooking(dataConnect, createBookingVars);

// Operation AddProperty:  For variables, look at type AddPropertyVars in ../index.d.ts
const { data } = await AddProperty(dataConnect, addPropertyVars);

// Operation AddReview:  For variables, look at type AddReviewVars in ../index.d.ts
const { data } = await AddReview(dataConnect, addReviewVars);

// Operation UpdateBookingStatus:  For variables, look at type UpdateBookingStatusVars in ../index.d.ts
const { data } = await UpdateBookingStatus(dataConnect, updateBookingStatusVars);

// Operation DeleteReview:  For variables, look at type DeleteReviewVars in ../index.d.ts
const { data } = await DeleteReview(dataConnect, deleteReviewVars);

// Operation GetMyProfile: 
const { data } = await GetMyProfile(dataConnect);

// Operation ListAllProperties: 
const { data } = await ListAllProperties(dataConnect);

// Operation GetPropertyDetails:  For variables, look at type GetPropertyDetailsVars in ../index.d.ts
const { data } = await GetPropertyDetails(dataConnect, getPropertyDetailsVars);


```