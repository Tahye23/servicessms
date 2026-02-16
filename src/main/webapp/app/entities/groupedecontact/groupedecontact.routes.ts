import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ASC } from 'app/config/navigation.constants';
import { GroupedecontactComponent } from './list/groupedecontact.component';
import { GroupedecontactDetailComponent } from './detail/groupedecontact-detail.component';
import { GroupedecontactUpdateComponent } from './update/groupedecontact-update.component';
import GroupedecontactResolve from './route/groupedecontact-routing-resolve.service';

const groupedecontactRoute: Routes = [
  {
    path: '',
    component: GroupedecontactComponent,
    data: {
      defaultSort: 'id,' + ASC,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: GroupedecontactDetailComponent,
    resolve: {
      groupedecontact: GroupedecontactResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: GroupedecontactUpdateComponent,
    resolve: {
      groupedecontact: GroupedecontactResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: GroupedecontactUpdateComponent,
    resolve: {
      groupedecontact: GroupedecontactResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default groupedecontactRoute;
