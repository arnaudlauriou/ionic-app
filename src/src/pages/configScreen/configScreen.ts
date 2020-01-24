import { Component } from '@angular/core';
import { ModalController, NavController, NavParams } from 'ionic-angular';
import {GeteduroamServices} from "../../providers/geteduroam-services/geteduroam-services";
import { ProfilePage } from '../profile/profile';
import { OauthFlow } from '../oauthFlow/oauthFlow';
import { LoadingProvider } from '../../providers/loading/loading';
import { InstitutionSearch } from '../institutionSearch/institutionSearch';
import { Plugins } from '@capacitor/core';
const { Keyboard } = Plugins;

@Component({
  selector: 'page-config-screen',
  templateUrl: 'configScreen.html',
})
export class ConfigurationScreen {

  showAll: boolean = false;

  /**
   * Set of available profiles
   */
  profiles: any;

  /**
   * All the institutions retrieved by the service [GeteduroamServices]{@link ../injectables/GeteduroamServices.html}
   */
  instances: any;

  /**
   * Set of institutions filtered by what is written in the search-bar
   */
  filteredInstances: any;

  /**
   * Selected institution
   */
  instance: any;

  /**
   * Name of the selected institution used in the search-bar
   */
  instanceName : any = '';

  /**
   * Selected profile
   */
  profile: any;

  /**
   * Default profile (if exists) in the selected institution profiles set
   */
  defaultProfile: any;

  /**
   * Name of the selected profile
   */
  profileName: any = '';

  /**
   * Id of the selected profile
   */
  selectedProfileId: any;

  /**
   * Property to decide whether or not to show the institutions list
   */
  showInstanceItems: boolean = false;

  /**
   * Property to show button
   */
  showButton: boolean = true;


  /**
   * Constructor
   * */
  constructor(public navCtrl: NavController, public navParams: NavParams, private getEduroamServices: GeteduroamServices,
              public loading: LoadingProvider, public modalCtrl: ModalController) {

  }

  /**
   * Method executes when the search bar is tapped.
   * */
  async showModal() {
    await Keyboard.hide();
    let searchModal = this.modalCtrl.create(InstitutionSearch, {
      instances: this.instances,
      instanceName: this.instanceName}
      );

    searchModal.onDidDismiss((data) => {

        if (data !== undefined) {
          this.instance = data;
          this.instanceName = data.name;

          this.initializeProfiles(this.instance);

        }
      });

      return await searchModal.present();

  }

  /**
   * Method which clears the profile after selecting a new institution or clear the selected one.
   * This method updates the properties [profile]{@link #profile}, [profileName]{@link #profileName} and [selectedProfileId]{@link #selectedProfileId}
   */
  clearProfile(){
    this.profile = '';
    this.profileName = '';
    this.selectedProfileId = '';
  }

  /**
   * Method which manages the selection of a new profile for the already selected institution.
   * This method updates the properties [profile]{@link #profile}, and [profileName]{@link #profileName}.
   */
  selectProfile(){
    let selectedProfile = this.profiles.filter((item:any) => {
      return (item.id == this.selectedProfileId);
    });
    this.profile = selectedProfile[0];
    this.profileName = this.profile.name;
  }

  /**
   * Method which manages the selection of a new profile for the already selected institution.
   * This method updates the property [profiles]{@link #profiles}.
   * @param {any} institution the selected institution.
   */
  initializeProfiles(institution: any) {
    this.profiles = institution.profiles;
    this.checkProfiles();
  }

  /**
   * Method which checks the profiles in case there is a default one or if there is only one profile.
   * This method updates the properties [profile]{@link #profile}, [profileName]{@link #profileName},
   * [selectedProfileId]{@link #selectedProfileId} and [defaultProfile]{@link #defaultProfile},
   */
  checkProfiles(){
    if(this.profiles.length === 1){
      this.profile = this.profiles[0];
      this.profileName = this.profile.name;
      this.selectedProfileId = this.profile.id;
      this.defaultProfile = '';
    } else {
      let filteredProfiles = this.profiles.filter((item:any) => {
        return (item.default == true);
      });
      this.defaultProfile = filteredProfiles[0];
      if(!!this.defaultProfile){
        this.profile = this.defaultProfile;
        this.profileName = this.profile.name;
        this.selectedProfileId = this.profile.id;
      }
    }
  }

  /**
   * Method which navigates to the following view.
   * If the selected profile is oauth, navigates to [OauthFlow]{OauthFlow}.
   * In other case, navigates to [ProfilePage]{ProfilePage} sending the selected [profile]{#profile}.
   */
  navigateTo(profile) {
    this.showAll = false;

    const toPage = !!profile.oauth ? OauthFlow : ProfilePage;
    this.navCtrl.push(toPage, {profile}, {animation: 'transition'});


  }

  /**
   * Method executed when the class is initialized.
   * This method updates the property [instances]{@link #instances} by making use of the service [GeteduroamServices]{@link ../injectables/GeteduroamServices.html}.
   */
  async ionViewDidEnter() {
    this.loading.createAndPresent();
    const response = await this.getEduroamServices.discovery();
    this.loading.dismiss();
    this.instances = response.instances;
    this.showAll = true;
  }
}
