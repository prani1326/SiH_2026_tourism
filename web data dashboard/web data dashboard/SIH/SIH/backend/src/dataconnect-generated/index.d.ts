import { ConnectorConfig, DataConnect, QueryRef, QueryPromise, ExecuteQueryOptions, MutationRef, MutationPromise, DataConnectSettings } from 'firebase/data-connect';

export const connectorConfig: ConnectorConfig;
export const dataConnectSettings: DataConnectSettings;

export type TimestampString = string;
export type UUIDString = string;
export type Int64String = string;
export type DateString = string;




export interface AddPropertyData {
  property_insert: Property_Key;
}

export interface AddPropertyVariables {
  title: string;
  description: string;
  pricePerNight: number;
  address: string;
}

export interface AddReviewData {
  review_insert: Review_Key;
}

export interface AddReviewVariables {
  propertyId: UUIDString;
  rating: number;
  comment: string;
}

export interface Availability_Key {
  id: UUIDString;
  __typename?: 'Availability_Key';
}

export interface Booking_Key {
  id: UUIDString;
  __typename?: 'Booking_Key';
}

export interface CreateBookingData {
  booking_insert: Booking_Key;
}

export interface CreateBookingVariables {
  propertyId: UUIDString;
  startDate: DateString;
  endDate: DateString;
  totalCost: number;
}

export interface DeleteReviewData {
  review_delete?: Review_Key | null;
}

export interface DeleteReviewVariables {
  id: UUIDString;
}

export interface GetMyProfileData {
  user?: {
    fullName: string;
    email: string;
    role: string;
  };
}

export interface GetPropertyDetailsData {
  property?: {
    title: string;
    description: string;
    amenities?: string[] | null;
    reviews_on_property: ({
      rating: number;
      comment: string;
    })[];
  };
}

export interface GetPropertyDetailsVariables {
  id: UUIDString;
}

export interface InsertUserData {
  user_insert: User_Key;
}

export interface ListAllPropertiesData {
  properties: ({
    title: string;
    pricePerNight: number;
    address: string;
    imageUrls?: string[] | null;
  })[];
}

export interface Property_Key {
  id: UUIDString;
  __typename?: 'Property_Key';
}

export interface Review_Key {
  id: UUIDString;
  __typename?: 'Review_Key';
}

export interface UpdateBookingStatusData {
  booking_update?: Booking_Key | null;
}

export interface UpdateBookingStatusVariables {
  id: UUIDString;
  status: string;
}

export interface UpsertAvailabilityData {
  availability_upsert: Availability_Key;
}

export interface UpsertAvailabilityVariables {
  date: DateString;
  isAvailable: boolean;
}

export interface User_Key {
  id: UUIDString;
  __typename?: 'User_Key';
}

interface InsertUserRef {
  /* Allow users to create refs without passing in DataConnect */
  (): MutationRef<InsertUserData, undefined>;
  /* Allow users to pass in custom DataConnect instances */
  (dc: DataConnect): MutationRef<InsertUserData, undefined>;
  operationName: string;
}
export const insertUserRef: InsertUserRef;

export function insertUser(): MutationPromise<InsertUserData, undefined>;
export function insertUser(dc: DataConnect): MutationPromise<InsertUserData, undefined>;

interface UpsertAvailabilityRef {
  /* Allow users to create refs without passing in DataConnect */
  (vars: UpsertAvailabilityVariables): MutationRef<UpsertAvailabilityData, UpsertAvailabilityVariables>;
  /* Allow users to pass in custom DataConnect instances */
  (dc: DataConnect, vars: UpsertAvailabilityVariables): MutationRef<UpsertAvailabilityData, UpsertAvailabilityVariables>;
  operationName: string;
}
export const upsertAvailabilityRef: UpsertAvailabilityRef;

export function upsertAvailability(vars: UpsertAvailabilityVariables): MutationPromise<UpsertAvailabilityData, UpsertAvailabilityVariables>;
export function upsertAvailability(dc: DataConnect, vars: UpsertAvailabilityVariables): MutationPromise<UpsertAvailabilityData, UpsertAvailabilityVariables>;

interface CreateBookingRef {
  /* Allow users to create refs without passing in DataConnect */
  (vars: CreateBookingVariables): MutationRef<CreateBookingData, CreateBookingVariables>;
  /* Allow users to pass in custom DataConnect instances */
  (dc: DataConnect, vars: CreateBookingVariables): MutationRef<CreateBookingData, CreateBookingVariables>;
  operationName: string;
}
export const createBookingRef: CreateBookingRef;

export function createBooking(vars: CreateBookingVariables): MutationPromise<CreateBookingData, CreateBookingVariables>;
export function createBooking(dc: DataConnect, vars: CreateBookingVariables): MutationPromise<CreateBookingData, CreateBookingVariables>;

interface AddPropertyRef {
  /* Allow users to create refs without passing in DataConnect */
  (vars: AddPropertyVariables): MutationRef<AddPropertyData, AddPropertyVariables>;
  /* Allow users to pass in custom DataConnect instances */
  (dc: DataConnect, vars: AddPropertyVariables): MutationRef<AddPropertyData, AddPropertyVariables>;
  operationName: string;
}
export const addPropertyRef: AddPropertyRef;

export function addProperty(vars: AddPropertyVariables): MutationPromise<AddPropertyData, AddPropertyVariables>;
export function addProperty(dc: DataConnect, vars: AddPropertyVariables): MutationPromise<AddPropertyData, AddPropertyVariables>;

interface AddReviewRef {
  /* Allow users to create refs without passing in DataConnect */
  (vars: AddReviewVariables): MutationRef<AddReviewData, AddReviewVariables>;
  /* Allow users to pass in custom DataConnect instances */
  (dc: DataConnect, vars: AddReviewVariables): MutationRef<AddReviewData, AddReviewVariables>;
  operationName: string;
}
export const addReviewRef: AddReviewRef;

export function addReview(vars: AddReviewVariables): MutationPromise<AddReviewData, AddReviewVariables>;
export function addReview(dc: DataConnect, vars: AddReviewVariables): MutationPromise<AddReviewData, AddReviewVariables>;

interface UpdateBookingStatusRef {
  /* Allow users to create refs without passing in DataConnect */
  (vars: UpdateBookingStatusVariables): MutationRef<UpdateBookingStatusData, UpdateBookingStatusVariables>;
  /* Allow users to pass in custom DataConnect instances */
  (dc: DataConnect, vars: UpdateBookingStatusVariables): MutationRef<UpdateBookingStatusData, UpdateBookingStatusVariables>;
  operationName: string;
}
export const updateBookingStatusRef: UpdateBookingStatusRef;

export function updateBookingStatus(vars: UpdateBookingStatusVariables): MutationPromise<UpdateBookingStatusData, UpdateBookingStatusVariables>;
export function updateBookingStatus(dc: DataConnect, vars: UpdateBookingStatusVariables): MutationPromise<UpdateBookingStatusData, UpdateBookingStatusVariables>;

interface DeleteReviewRef {
  /* Allow users to create refs without passing in DataConnect */
  (vars: DeleteReviewVariables): MutationRef<DeleteReviewData, DeleteReviewVariables>;
  /* Allow users to pass in custom DataConnect instances */
  (dc: DataConnect, vars: DeleteReviewVariables): MutationRef<DeleteReviewData, DeleteReviewVariables>;
  operationName: string;
}
export const deleteReviewRef: DeleteReviewRef;

export function deleteReview(vars: DeleteReviewVariables): MutationPromise<DeleteReviewData, DeleteReviewVariables>;
export function deleteReview(dc: DataConnect, vars: DeleteReviewVariables): MutationPromise<DeleteReviewData, DeleteReviewVariables>;

interface GetMyProfileRef {
  /* Allow users to create refs without passing in DataConnect */
  (): QueryRef<GetMyProfileData, undefined>;
  /* Allow users to pass in custom DataConnect instances */
  (dc: DataConnect): QueryRef<GetMyProfileData, undefined>;
  operationName: string;
}
export const getMyProfileRef: GetMyProfileRef;

export function getMyProfile(options?: ExecuteQueryOptions): QueryPromise<GetMyProfileData, undefined>;
export function getMyProfile(dc: DataConnect, options?: ExecuteQueryOptions): QueryPromise<GetMyProfileData, undefined>;

interface ListAllPropertiesRef {
  /* Allow users to create refs without passing in DataConnect */
  (): QueryRef<ListAllPropertiesData, undefined>;
  /* Allow users to pass in custom DataConnect instances */
  (dc: DataConnect): QueryRef<ListAllPropertiesData, undefined>;
  operationName: string;
}
export const listAllPropertiesRef: ListAllPropertiesRef;

export function listAllProperties(options?: ExecuteQueryOptions): QueryPromise<ListAllPropertiesData, undefined>;
export function listAllProperties(dc: DataConnect, options?: ExecuteQueryOptions): QueryPromise<ListAllPropertiesData, undefined>;

interface GetPropertyDetailsRef {
  /* Allow users to create refs without passing in DataConnect */
  (vars: GetPropertyDetailsVariables): QueryRef<GetPropertyDetailsData, GetPropertyDetailsVariables>;
  /* Allow users to pass in custom DataConnect instances */
  (dc: DataConnect, vars: GetPropertyDetailsVariables): QueryRef<GetPropertyDetailsData, GetPropertyDetailsVariables>;
  operationName: string;
}
export const getPropertyDetailsRef: GetPropertyDetailsRef;

export function getPropertyDetails(vars: GetPropertyDetailsVariables, options?: ExecuteQueryOptions): QueryPromise<GetPropertyDetailsData, GetPropertyDetailsVariables>;
export function getPropertyDetails(dc: DataConnect, vars: GetPropertyDetailsVariables, options?: ExecuteQueryOptions): QueryPromise<GetPropertyDetailsData, GetPropertyDetailsVariables>;

