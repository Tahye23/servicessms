import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ASC } from 'app/config/navigation.constants';
import { PartnershipRequestListComponent } from './list/partnershipRequestService.component';
import { PartnershipRequestDetailComponent } from './partnershipRequestDetail/partnershipRequestDetailComponent.component';

const partnerShipRequestRoute: Routes = [
  {
    path: '',
    component: PartnershipRequestListComponent,

    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: PartnershipRequestDetailComponent,

    canActivate: [UserRouteAccessService],
  },
];

export default partnerShipRequestRoute;
