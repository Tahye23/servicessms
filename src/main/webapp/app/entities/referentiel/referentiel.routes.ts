import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ASC } from 'app/config/navigation.constants';
import { ReferentielComponent } from './list/referentiel.component';
import { ReferentielDetailComponent } from './detail/referentiel-detail.component';
import { ReferentielUpdateComponent } from './update/referentiel-update.component';
import ReferentielResolve from './route/referentiel-routing-resolve.service';

const referentielRoute: Routes = [
  {
    path: '',
    component: ReferentielComponent,
    data: {
      defaultSort: 'id,' + ASC,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: ReferentielDetailComponent,
    resolve: {
      referentiel: ReferentielResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: ReferentielUpdateComponent,
    resolve: {
      referentiel: ReferentielResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: ReferentielUpdateComponent,
    resolve: {
      referentiel: ReferentielResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default referentielRoute;
