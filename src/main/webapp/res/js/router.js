
var PageRouter = Backbone.Router.extend({
	
	routes: {
		"":								"start",
		"start":						"start",
		"create":						"create",
		"about":						"about",
		"archive/:archiveId":			"archive",
	},
	
	initialize: function(options) {
		
		// init views
		workspaceArchives = new ArchiveCollection();
		navigationView = new NavigationView({ collection: workspaceArchives });
		archiveView = new ArchiveView();
		startView = new StartView();
		createView = new CreateView();
		aboutView = new AboutView();
		messageView = new MessageView();
		
		workspaceArchives.once("sync", function(eventName) {
			Backbone.history.start();
		});
		navigationView.fetch();
	},
	
	// Helper functions
	selectArchive: function( archiveId ) {
		return this.navigate( "archive/" + archiveId, {trigger: true} );
	},
	selectArchiveFile: function( archiveId, fileId ) {
		if( archiveId == undefined ) {
			fileId = archiveId;
			archiveId = archiveView.model.get("id");
		}
		 
		return this.navigate( "archive/" + archiveId + "/" + fileId );
	},
	goToPage: function( page ) {
		return this.navigate( page, {trigger: true} );
	},
	
	// Route functions
	start: function() {
		navigationView.goToPage("start-page", false);
	},
	create: function() {
		navigationView.goToPage("create-page", false);
	},
	about: function() {
		navigationView.goToPage("about-page", false);
	},
	archive: function(archiveId) {
		navigationView.selectArchive(archiveId, false);
	}
	
});