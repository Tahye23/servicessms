import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ASC } from 'app/config/navigation.constants';
import { OTPStorageComponent } from './list/otp-storage.component';
import { OTPStorageDetailComponent } from './detail/otp-storage-detail.component';
import { OTPStorageUpdateComponent } from './update/otp-storage-update.component';
import OTPStorageResolve from './route/otp-storage-routing-resolve.service';

const oTPStorageRoute: Routes = [
  {
    path: '',
    component: OTPStorageComponent,
    data: {
      defaultSort: 'id,' + ASC,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: OTPStorageDetailComponent,
    resolve: {
      oTPStorage: OTPStorageResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: OTPStorageUpdateComponent,
    resolve: {
      oTPStorage: OTPStorageResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: OTPStorageUpdateComponent,
    resolve: {
      oTPStorage: OTPStorageResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default oTPStorageRoute;
